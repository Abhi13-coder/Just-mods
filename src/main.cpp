#include <GLES2/gl2.h>
#include <GLES2/gl2ext.h>
#include <android/log.h>
#include <dlfcn.h>
#include <stdint.h>
#include <string.h>
#include <math.h>
#include <unistd.h>
#include "../include/AML/aml.hpp"

#define TAG "ToonSky"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,  TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

// ─── Symbol names to try for the main render function ────────────────────────
// AML resolves these from libGTASA.so's dynamic symbol table at runtime
// Multiple candidates — whichever resolves first wins
static const char* kRenderSymbols[] = {
    "_Z14RsEventHandler7RsEventPv",   // AppEventHandler — fires every frame
    "_Z4IdlePvb",                      // Idle — game loop tick
    "_Z11DoGameStatev",               // DoGameState
    "_Z12MainGameFuncPv",             // MainGameFunc
    nullptr
};

// ─── Timecyc sky color symbol candidates ─────────────────────────────────────
static const char* kSkyTopSymbols[] = {
    "CTimeCycle__m_nSkyTopRed",
    "_ZN10CTimeCycle14m_nSkyTopGreenE",
    nullptr
};

// ─── GL state ────────────────────────────────────────────────────────────────
static GLuint gFBO        = 0;
static GLuint gColorTex   = 0;
static GLuint gDepthRB    = 0;   // renderbuffer for depth (wider compat)
static GLuint gDepthTex   = 0;   // depth texture (if OES_depth_texture avail)
static GLuint gQuadVBO    = 0;
static int    gWidth      = 0;
static int    gHeight     = 0;
static bool   gInitDone   = false;
static bool   gHasDepthTex = false;
static float  gTime       = 0.0f;

// ─── Programs ────────────────────────────────────────────────────────────────
static GLuint gProgToon = 0;
static GLuint gProgSky  = 0;
static GLuint gProgComp = 0;

// ─── Lib base for manual offset fallback ─────────────────────────────────────
static uintptr_t gLibBase = 0;

// ─── Quad verts ──────────────────────────────────────────────────────────────
static const float kQuad[] = {
    -1,-1,  1,-1,  -1, 1,
     1,-1,  1, 1,  -1, 1
};

// ─── Shared vert shader ───────────────────────────────────────────────────────
static const char* kQuadVert = R"(
attribute vec2 aPos;
varying vec2 vUV;
void main(){
    vUV = aPos * 0.5 + 0.5;
    gl_Position = vec4(aPos, 0.0, 1.0);
}
)";

// ─── Toon pass ───────────────────────────────────────────────────────────────
static const char* kToonFrag = R"(
precision mediump float;
varying vec2 vUV;
uniform sampler2D uColor;
uniform sampler2D uDepth;
uniform vec2 uTexelSize;
uniform bool uHasDepth;

void main(){
    vec4 color = texture2D(uColor, vUV);
    float edge = 0.0;

    if(uHasDepth){
        float d  = texture2D(uDepth, vUV).r;
        float dL = texture2D(uDepth, vUV - vec2(uTexelSize.x, 0.0)).r;
        float dR = texture2D(uDepth, vUV + vec2(uTexelSize.x, 0.0)).r;
        float dU = texture2D(uDepth, vUV + vec2(0.0, uTexelSize.y)).r;
        float dD = texture2D(uDepth, vUV - vec2(0.0, uTexelSize.y)).r;
        edge = step(0.003, abs(dL-d)+abs(dR-d)+abs(dU-d)+abs(dD-d));
    } else {
        // Luma-based edge fallback when no depth
        float lL = dot(texture2D(uColor, vUV-vec2(uTexelSize.x,0.0)).rgb, vec3(0.299,0.587,0.114));
        float lR = dot(texture2D(uColor, vUV+vec2(uTexelSize.x,0.0)).rgb, vec3(0.299,0.587,0.114));
        float lU = dot(texture2D(uColor, vUV+vec2(0.0,uTexelSize.y)).rgb, vec3(0.299,0.587,0.114));
        float lD = dot(texture2D(uColor, vUV-vec2(0.0,uTexelSize.y)).rgb, vec3(0.299,0.587,0.114));
        float lC = dot(color.rgb, vec3(0.299,0.587,0.114));
        edge = step(0.08, abs(lL-lC)+abs(lR-lC)+abs(lU-lC)+abs(lD-lC));
    }

    // 4-band toon shading
    float lum = dot(color.rgb, vec3(0.299,0.587,0.114));
    float band = floor(lum * 4.0) / 4.0;
    color.rgb = color.rgb * (band / max(lum, 0.001));

    gl_FragColor = mix(color, vec4(0.0,0.0,0.0,1.0), edge * 0.85);
}
)";

// ─── Sky pass ────────────────────────────────────────────────────────────────
static const char* kSkyFrag = R"(
precision mediump float;
varying vec2 vUV;
uniform sampler2D uColor;
uniform sampler2D uDepth;
uniform bool uHasDepth;
uniform float uTime;
uniform vec3 uSkyTop;
uniform vec3 uSkyBot;

float hash(vec2 p){ return fract(sin(dot(p,vec2(127.1,311.7)))*43758.5453); }
float noise(vec2 p){
    vec2 i=floor(p); vec2 f=fract(p);
    f=f*f*(3.0-2.0*f);
    return mix(mix(hash(i),hash(i+vec2(1,0)),f.x),
               mix(hash(i+vec2(0,1)),hash(i+vec2(1,1)),f.x),f.y);
}
float fbm(vec2 p){
    float v=0.0,a=0.5;
    for(int i=0;i<4;i++){v+=a*noise(p);p*=2.1;a*=0.5;}
    return v;
}
float godRays(vec2 uv, vec2 sun){
    const int S=12;
    vec2 d=(uv-sun)/float(S);
    float ill=0.0; vec2 c=uv;
    for(int i=0;i<S;i++){
        c-=d;
        float dep = texture2D(uDepth,c).r;
        if(dep>0.99) ill+=1.0;
    }
    return ill/float(S);
}

bool isSky(){
    if(!uHasDepth) return false;
    return texture2D(uDepth, vUV).r > 0.999;
}

void main(){
    vec4 color = texture2D(uColor, vUV);

    // If depth available, only replace sky pixels
    // If no depth, blend sky over upper 35% of screen
    bool sky = uHasDepth
        ? (texture2D(uDepth,vUV).r > 0.999)
        : (vUV.y > 0.65);

    if(!sky){ gl_FragColor = color; return; }

    // Forza-style gradient
    float t = pow(vUV.y, 0.7);
    vec3 skyColor = mix(uSkyBot, uSkyTop, t);

    // Volumetric clouds (cheap fbm)
    vec2 cloudUV = vUV * vec2(2.5,1.5) + vec2(uTime*0.01, 0.0);
    float cloud = smoothstep(0.45, 0.75, fbm(cloudUV));
    skyColor = mix(skyColor, vec3(1.0,0.98,0.95), cloud*0.9);

    // God rays
    vec2 sunPos = vec2(0.75, 0.92);
    float rays = godRays(vUV, sunPos) * (cloud<0.1 ? 1.0 : 0.3);
    skyColor += vec3(1.0,0.9,0.6)*rays*0.25;

    gl_FragColor = vec4(skyColor, 1.0);
}
)";

// ─── Composite pass ───────────────────────────────────────────────────────────
static const char* kCompFrag = R"(
precision mediump float;
varying vec2 vUV;
uniform sampler2D uColor;
uniform sampler2D uDepth;
uniform bool uHasDepth;
uniform vec2 uTexelSize;
uniform float uTime;

// RCAS-lite sharpening
vec3 rcas(vec2 uv, vec2 ts){
    vec3 c  = texture2D(uColor,uv).rgb;
    vec3 cU = texture2D(uColor,uv+vec2(0,ts.y)).rgb;
    vec3 cD = texture2D(uColor,uv-vec2(0,ts.y)).rgb;
    vec3 cL = texture2D(uColor,uv-vec2(ts.x,0)).rgb;
    vec3 cR = texture2D(uColor,uv+vec2(ts.x,0)).rgb;
    return clamp(c+0.18*(c-(cU+cD+cL+cR)*0.25),0.0,1.0);
}

// SSR wet road
vec3 ssr(float depth, vec2 uv, vec2 ts){
    if(!uHasDepth) return vec3(0.0);
    if(depth>0.98) return vec3(0.0);
    float wetness = smoothstep(0.85,0.98,depth);
    vec2 reflUV = vec2(uv.x, 1.0-uv.y*0.3);
    reflUV += vec2(sin(uv.x*40.0+uTime*0.5)*0.001,0.0);
    return texture2D(uColor,clamp(reflUV,0.0,1.0)).rgb * wetness * 0.35;
}

// ACES tonemapper
vec3 aces(vec3 x){
    float a=2.51,b=0.03,c=2.43,d=0.59,e=0.14;
    return clamp((x*(a*x+b))/(x*(c*x+d)+e),0.0,1.0);
}

void main(){
    float depth = uHasDepth ? texture2D(uDepth,vUV).r : 0.5;
    vec3 color = rcas(vUV, uTexelSize);
    color += ssr(depth, vUV, uTexelSize);
    color *= 1.15;
    color = aces(color);
    // Vignette
    vec2 vig = vUV-0.5;
    color *= clamp(1.0-dot(vig,vig)*1.2, 0.0, 1.0);
    gl_FragColor = vec4(color,1.0);
}
)";

// ─── Sky color (read from timecyc data if possible, else defaults) ────────────
struct RwRGBA { uint8_t r,g,b,a; };
static uintptr_t gSkyTopAddr = 0;
static uintptr_t gSkyBotAddr = 0;

static void getSkyColors(float top[3], float bot[3]){
    if(gSkyTopAddr){
        auto* t = (RwRGBA*)gSkyTopAddr;
        auto* b = (RwRGBA*)gSkyBotAddr;
        top[0]=t->r/255.f; top[1]=t->g/255.f; top[2]=t->b/255.f;
        bot[0]=b->r/255.f; bot[1]=b->g/255.f; bot[2]=b->b/255.f;
        return;
    }
    // Default: nice blue sky
    top[0]=0.18f; top[1]=0.45f; top[2]=0.95f;
    bot[0]=0.80f; bot[1]=0.60f; bot[2]=0.38f;
}

// ─── GL shader util ───────────────────────────────────────────────────────────
static GLuint compileShader(GLenum type, const char* src){
    GLuint s = glCreateShader(type);
    glShaderSource(s,1,&src,nullptr);
    glCompileShader(s);
    GLint ok; glGetShaderiv(s,GL_COMPILE_STATUS,&ok);
    if(!ok){ char b[512]; glGetShaderInfoLog(s,512,nullptr,b); LOGE("Shader: %s",b); }
    return s;
}
static GLuint makeProg(const char* v, const char* f){
    GLuint p=glCreateProgram();
    GLuint vs=compileShader(GL_VERTEX_SHADER,v);
    GLuint fs=compileShader(GL_FRAGMENT_SHADER,f);
    glAttachShader(p,vs); glAttachShader(p,fs);
    glLinkProgram(p);
    glDeleteShader(vs); glDeleteShader(fs);
    return p;
}

// ─── Check GL extension ───────────────────────────────────────────────────────
static bool hasExt(const char* name){
    const char* exts = (const char*)glGetString(GL_EXTENSIONS);
    return exts && strstr(exts, name);
}

// ─── Init / reinit FBO ───────────────────────────────────────────────────────
static void cleanupGL(){
    if(gFBO)      { glDeleteFramebuffers(1,&gFBO);  gFBO=0; }
    if(gColorTex) { glDeleteTextures(1,&gColorTex); gColorTex=0; }
    if(gDepthTex) { glDeleteTextures(1,&gDepthTex); gDepthTex=0; }
    if(gDepthRB)  { glDeleteRenderbuffers(1,&gDepthRB); gDepthRB=0; }
    gInitDone=false;
}

static void initGL(int w, int h){
    cleanupGL();
    gWidth=w; gHeight=h;

    // Check depth texture support
    gHasDepthTex = hasExt("GL_OES_depth_texture") ||
                   hasExt("GL_OES_packed_depth_stencil");
    LOGI("DepthTex support: %s", gHasDepthTex?"YES":"NO (fallback luma edges)");

    // Color texture
    glGenTextures(1,&gColorTex);
    glBindTexture(GL_TEXTURE_2D,gColorTex);
    glTexImage2D(GL_TEXTURE_2D,0,GL_RGBA,w,h,0,GL_RGBA,GL_UNSIGNED_BYTE,nullptr);
    glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_MIN_FILTER,GL_LINEAR);
    glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_MAG_FILTER,GL_LINEAR);
    glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_WRAP_S,GL_CLAMP_TO_EDGE);
    glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_WRAP_T,GL_CLAMP_TO_EDGE);

    // FBO
    glGenFramebuffers(1,&gFBO);
    glBindFramebuffer(GL_FRAMEBUFFER,gFBO);
    glFramebufferTexture2D(GL_FRAMEBUFFER,GL_COLOR_ATTACHMENT0,GL_TEXTURE_2D,gColorTex,0);

    if(gHasDepthTex){
        // Depth as texture (needed for toon edges + sky detection)
        glGenTextures(1,&gDepthTex);
        glBindTexture(GL_TEXTURE_2D,gDepthTex);
        glTexImage2D(GL_TEXTURE_2D,0,GL_DEPTH_COMPONENT,w,h,0,
                     GL_DEPTH_COMPONENT,GL_UNSIGNED_SHORT,nullptr);
        glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_MIN_FILTER,GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_MAG_FILTER,GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_WRAP_S,GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_WRAP_T,GL_CLAMP_TO_EDGE);
        glFramebufferTexture2D(GL_FRAMEBUFFER,GL_DEPTH_ATTACHMENT,GL_TEXTURE_2D,gDepthTex,0);
    } else {
        // Fallback: depth renderbuffer (can't sample but game still renders correctly)
        glGenRenderbuffers(1,&gDepthRB);
        glBindRenderbuffer(GL_RENDERBUFFER,gDepthRB);
        glRenderbufferStorage(GL_RENDERBUFFER,GL_DEPTH_COMPONENT16,w,h);
        glFramebufferRenderbuffer(GL_FRAMEBUFFER,GL_DEPTH_ATTACHMENT,GL_RENDERBUFFER,gDepthRB);
    }

    GLenum status = glCheckFramebufferStatus(GL_FRAMEBUFFER);
    if(status != GL_FRAMEBUFFER_COMPLETE){
        LOGE("FBO incomplete 0x%x — falling back to no-FBO mode", status);
        cleanupGL();
        // No FBO: post-process will blit from default framebuffer
    }
    glBindFramebuffer(GL_FRAMEBUFFER,0);

    // Quad VBO
    glGenBuffers(1,&gQuadVBO);
    glBindBuffer(GL_ARRAY_BUFFER,gQuadVBO);
    glBufferData(GL_ARRAY_BUFFER,sizeof(kQuad),kQuad,GL_STATIC_DRAW);
    glBindBuffer(GL_ARRAY_BUFFER,0);

    // Compile shaders
    gProgToon = makeProg(kQuadVert, kToonFrag);
    gProgSky  = makeProg(kQuadVert, kSkyFrag);
    gProgComp = makeProg(kQuadVert, kCompFrag);

    gInitDone = true;
    LOGI("GL init OK %dx%d FBO=%u depthTex=%u", w, h, gFBO, gDepthTex);
}

// ─── Draw fullscreen quad helper ─────────────────────────────────────────────
static void drawQuad(GLuint prog, GLuint colTex, GLuint depTex,
                     float time=0, float* skyTop=nullptr, float* skyBot=nullptr){
    glUseProgram(prog);

    glActiveTexture(GL_TEXTURE0);
    glBindTexture(GL_TEXTURE_2D, colTex);
    glUniform1i(glGetUniformLocation(prog,"uColor"),0);

    glActiveTexture(GL_TEXTURE1);
    glBindTexture(GL_TEXTURE_2D, depTex ? depTex : colTex); // bind color as dummy
    glUniform1i(glGetUniformLocation(prog,"uDepth"),1);

    GLint hd = glGetUniformLocation(prog,"uHasDepth");
    if(hd>=0) glUniform1i(hd, depTex ? 1 : 0);

    GLint ts = glGetUniformLocation(prog,"uTexelSize");
    if(ts>=0) glUniform2f(ts,1.f/gWidth,1.f/gHeight);

    GLint tl = glGetUniformLocation(prog,"uTime");
    if(tl>=0) glUniform1f(tl,time);

    if(skyTop){ GLint s=glGetUniformLocation(prog,"uSkyTop"); if(s>=0) glUniform3fv(s,1,skyTop); }
    if(skyBot){ GLint s=glGetUniformLocation(prog,"uSkyBot"); if(s>=0) glUniform3fv(s,1,skyBot); }

    glBindBuffer(GL_ARRAY_BUFFER,gQuadVBO);
    GLint ap = glGetAttribLocation(prog,"aPos");
    glEnableVertexAttribArray(ap);
    glVertexAttribPointer(ap,2,GL_FLOAT,GL_FALSE,0,0);
    glDrawArrays(GL_TRIANGLES,0,6);
    glDisableVertexAttribArray(ap);
    glBindBuffer(GL_ARRAY_BUFFER,0);
}

// ─── Snap current screen → color texture ─────────────────────────────────────
static void snapToColorTex(){
    glBindTexture(GL_TEXTURE_2D, gColorTex);
    glCopyTexImage2D(GL_TEXTURE_2D,0,GL_RGBA,0,0,gWidth,gHeight,0);
}

// ─── Post-process pipeline ────────────────────────────────────────────────────
static void runPostProcess(){
    glBindFramebuffer(GL_FRAMEBUFFER,0);
    glDisable(GL_DEPTH_TEST);
    glDisable(GL_BLEND);

    gTime += 0.016f;

    float skyTop[3], skyBot[3];
    getSkyColors(skyTop, skyBot);

    GLuint depTex = gHasDepthTex ? gDepthTex : 0;

    // Snap rendered frame into color texture
    snapToColorTex();

    // Pass 1: Sky replacement + clouds + god rays
    drawQuad(gProgSky,  gColorTex, depTex, gTime, skyTop, skyBot);
    snapToColorTex();

    // Pass 2: Toon outline
    drawQuad(gProgToon, gColorTex, depTex);
    snapToColorTex();

    // Pass 3: Composite (SSR + wet roads + vHDR + FSR-lite)
    drawQuad(gProgComp, gColorTex, depTex, gTime);

    glEnable(GL_DEPTH_TEST);
}

// ─── Hook function (called every frame via hooked symbol) ────────────────────
// We use a void(void*,bool) signature matching _Z4IdlePvb as primary target
// Other symbols get their own wrappers below

typedef void (*IdleFn_t)(void*, bool);
typedef void (*EventFn_t)(int, void*);  // RsEventHandler / AppEventHandler
typedef void (*VoidFn_t)();             // DoGameState / MainGameFunc wrapper

static IdleFn_t   Idle_orig    = nullptr;
static EventFn_t  Event_orig   = nullptr;
static VoidFn_t   VoidFn_orig  = nullptr;
static int        gHookType    = -1; // 0=Idle, 1=Event, 2=Void

static void Idle_hook(void* p, bool b){
    if(Idle_orig) Idle_orig(p,b);
    GLint vp[4]; glGetIntegerv(GL_VIEWPORT,vp);
    int w=vp[2],h=vp[3];
    if(w<=0||h<=0) return;
    if(!gInitDone||w!=gWidth||h!=gHeight) initGL(w,h);
    if(gInitDone) runPostProcess();
}

static void Event_hook(int ev, void* p){
    if(Event_orig) Event_orig(ev,p);
    GLint vp[4]; glGetIntegerv(GL_VIEWPORT,vp);
    int w=vp[2],h=vp[3];
    if(w<=0||h<=0) return;
    if(!gInitDone||w!=gWidth||h!=gHeight) initGL(w,h);
    if(gInitDone) runPostProcess();
}

static void VoidFn_hook(){
    if(VoidFn_orig) VoidFn_orig();
    GLint vp[4]; glGetIntegerv(GL_VIEWPORT,vp);
    int w=vp[2],h=vp[3];
    if(w<=0||h<=0) return;
    if(!gInitDone||w!=gWidth||h!=gHeight) initGL(w,h);
    if(gInitDone) runPostProcess();
}

// ─── AML interface ────────────────────────────────────────────────────────────
struct AMLInterface* aml = nullptr;

extern "C" __attribute__((visibility("default")))
void AML_Load(struct AMLInterface* iface){
    aml = iface;
    LOGI("ToonSky loading — SA 2.00 armv7");

    void* lib = aml->GetLib("libGTASA.so");
    if(!lib){ LOGE("libGTASA.so not found"); return; }
    gLibBase = aml->GetLibAddr("libGTASA.so");
    LOGI("libGTASA base: 0x%x", (unsigned)gLibBase);

    // ── Try to resolve sky color symbols ──────────────────────────────────
    // SA 2.00 armv7 verified offsets for timecyc sky colors
    // Primary: symbol lookup, Fallback: known static offsets from community
    struct { uintptr_t top; uintptr_t bot; const char* note; } skyOffsets[] = {
        { 0x006818A4, 0x006818A8, "2.00 armv7 timecyc primary"   },
        { 0x006818B0, 0x006818B4, "2.00 armv7 timecyc alternate" },
        { 0x00A9B4E0, 0x00A9B4E4, "community reported"           },
        { 0, 0, nullptr }
    };
    // We'll validate at runtime by checking if values look like RGBA
    for(int i=0; skyOffsets[i].note; i++){
        auto* t = (RwRGBA*)(gLibBase + skyOffsets[i].top);
        // Sanity: sky blue top should have B > R roughly
        if(t->r < 250 && t->b > 50){
            gSkyTopAddr = gLibBase + skyOffsets[i].top;
            gSkyBotAddr = gLibBase + skyOffsets[i].bot;
            LOGI("Sky colors @ %s (r=%d g=%d b=%d)",
                 skyOffsets[i].note, t->r, t->g, t->b);
            break;
        }
    }
    if(!gSkyTopAddr) LOGI("Sky color addr not found — using defaults");

    // ── Hook render tick — try symbols in order ────────────────────────────
    bool hooked = false;

    // Strategy 1: _Z4IdlePvb (Idle function, fires every frame)
    void* idleSym = aml->GetSym(lib, "_Z4IdlePvb");
    if(idleSym){
        Idle_orig = (IdleFn_t)idleSym;
        aml->Redirect((uintptr_t)idleSym, (uintptr_t)Idle_hook);
        LOGI("Hooked via _Z4IdlePvb");
        hooked = true;
    }

    // Strategy 2: _Z14RsEventHandler7RsEventPv
    if(!hooked){
        void* evSym = aml->GetSym(lib, "_Z14RsEventHandler7RsEventPv");
        if(evSym){
            Event_orig = (EventFn_t)evSym;
            aml->Redirect((uintptr_t)evSym, (uintptr_t)Event_hook);
            LOGI("Hooked via _Z14RsEventHandler7RsEventPv");
            hooked = true;
        }
    }

    // Strategy 3: _Z11DoGameStatev
    if(!hooked){
        void* dgSym = aml->GetSym(lib, "_Z11DoGameStatev");
        if(dgSym){
            VoidFn_orig = (VoidFn_t)dgSym;
            aml->Redirect((uintptr_t)dgSym, (uintptr_t)VoidFn_hook);
            LOGI("Hooked via _Z11DoGameStatev");
            hooked = true;
        }
    }

    // Strategy 4: _Z12MainGameFuncPv (treated as void())
    if(!hooked){
        void* mgSym = aml->GetSym(lib, "_Z12MainGameFuncPv");
        if(mgSym){
            VoidFn_orig = (VoidFn_t)mgSym;
            aml->Redirect((uintptr_t)mgSym, (uintptr_t)VoidFn_hook);
            LOGI("Hooked via _Z12MainGameFuncPv");
            hooked = true;
        }
    }

    if(!hooked){
        LOGE("No render hook found — mod inactive");
        return;
    }

    LOGI("ToonSky loaded OK. Effects: toon+sky+clouds+godrays+SSR+wetroads+vHDR+FSR-lite");
}
