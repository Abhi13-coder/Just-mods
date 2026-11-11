(()=>{'use strict';
const $=s=>document.querySelector(s),root=document.documentElement;
const secs=[...document.querySelectorAll('.stn')],N=secs.length;
const reduce=matchMedia('(prefers-reduced-motion: reduce)').matches;
let ok=false;
try{const c=document.createElement('canvas');ok=!!(window.WebGLRenderingContext&&(c.getContext('webgl')||c.getContext('experimental-webgl')))&&!!window.THREE}catch(e){}
if(!ok){root.classList.add('nogl');return}
root.classList.add('gl');
const T=THREE,small=innerWidth<700,cv=$('#scene'),bar=$('#bar'),hud=$('#hud');
const r=new T.WebGLRenderer({canvas:cv,antialias:!small,powerPreference:'low-power'});
r.setPixelRatio(Math.min(devicePixelRatio||1,small?1.25:2));
const sc=new T.Scene();sc.background=new T.Color(0x0b1118);sc.fog=new T.Fog(0x0b1118,40,170);
const cam=new T.PerspectiveCamera(55,1,.1,300);
sc.add(new T.HemisphereLight(0x9bb8d0,0x1a2028,1));
const sun=new T.DirectionalLight(0xfff1dc,1.5);sun.position.set(30,50,20);sc.add(sun);
const M=(c,o)=>new T.MeshStandardMaterial(Object.assign({color:c,flatShading:true,roughness:.7,metalness:.2},o));
const hull=M(0xc9d1d9),dark=M(0x2a3440),solar=M(0x1b3a63,{metalness:.6,roughness:.4});
const lineM=new T.LineBasicMaterial({color:0x8a97a6});
const box=(w,h,d,m,x=0,y=0,z=0)=>{const o=new T.Mesh(new T.BoxGeometry(w,h,d),m);o.position.set(x,y,z);return o};
let hi=null;

function station(sec,i){
  const acc=new T.Color(sec.dataset.accent),g=new T.Group(),mods=[];
  const n=sec.querySelectorAll('.mods li').length,len=10+n*2.2,h=1.1;
  g.add(box(.7,.7,len,hull));
  const p=[],q=[[-h,-h],[h,-h],[h,h],[-h,h]];
  for(let z=-len/2;z<=len/2+.01;z+=1.6){
    q.forEach((a,k)=>{const b=q[(k+1)%4];p.push(a[0],a[1],z,b[0],b[1],z)});
    if(z+1.6<=len/2+.01){q.forEach(a=>p.push(a[0],a[1],z,a[0],a[1],z+1.6));p.push(-h,-h,z,h,h,z+1.6)}
  }
  const lg=new T.BufferGeometry();lg.setAttribute('position',new T.Float32BufferAttribute(p,3));
  g.add(new T.LineSegments(lg,lineM));
  for(let j=0;j<n;j++){
    const side=j%2?1:-1,z=-len/2+2+j*(len-4)/Math.max(n-1,1),k=(i+j)%3,mg=new T.Group();
    let body,top;
    if(k===0){body=new T.Mesh(new T.CylinderGeometry(1.3,1.3,3.2,8),hull);body.rotation.z=Math.PI/2;top=1.3}
    else if(k===1){body=box(3,2.2,2.8,hull);top=1.1}
    else{body=new T.Mesh(new T.CylinderGeometry(1.2,1.2,3,8),hull);top=1.5}
    const em=M(0x111111,{emissive:acc,emissiveIntensity:.5}),win=box(2,.12,.9,em,0,top+.06,0);
    const dock=new T.Mesh(new T.CylinderGeometry(.25,.25,2.2,6),dark);dock.rotation.z=Math.PI/2;dock.position.x=-side*2.7;
    mg.add(body,win,dock);mg.position.set(side*4.8,(j%3-1)*1.4,z);
    g.add(mg);mods.push({g:mg,em});
  }
  g.add(box(7.4,.15,.15,dark,0,0,len/2+1.2),box(3,.08,2.4,solar,-2.6,0,len/2+1.2),box(3,.08,2.4,solar,2.6,0,len/2+1.2));
  const ant=new T.Mesh(new T.CylinderGeometry(.06,.06,3.5,4),dark);ant.position.set(0,1.9,-len/2);g.add(ant);
  const dish=new T.Mesh(new T.SphereGeometry(.9,8,4,0,Math.PI*2,0,Math.PI/2),M(0xdde3ea,{side:T.DoubleSide}));
  dish.position.set(0,3.6,-len/2);dish.rotation.x=-1.1;g.add(dish);
  const lamp=(c,x,z)=>box(.25,.25,.25,M(0x000000,{emissive:c,emissiveIntensity:1.5}),x,-h,z);
  g.add(lamp(0xff3b30,-h,-len/2),lamp(0x34c759,h,-len/2),lamp(acc,0,len/2));
  return {g,mods};
}
const S=secs.map((_,i)=>new T.Vector3(Math.sin(i*1.4)*24,(i%2?8:-6)+i*1.5,-60-i*95));
const pts=[new T.Vector3(0,6,35)];
S.forEach(s=>{pts.push(s.clone().add(new T.Vector3(14,3,25)),s.clone().add(new T.Vector3(12,1,0)),s.clone().add(new T.Vector3(4,0,-28)))});
const curve=new T.CatmullRomCurve3(pts,false,'centripetal'),MP=pts.length;
const ts=[0,...secs.map((_,i)=>(2+3*i)/(MP-1)),1];
const st=secs.map((s,i)=>{const o=station(s,i);o.g.position.copy(S[i]);o.g.rotation.y=(i-2)*.3;sc.add(o.g);
  s.querySelectorAll('.mods li').forEach((li,j)=>{const on=()=>{hi=[i,j]},off=()=>{hi=null};li.addEventListener('mouseenter',on);li.addEventListener('focus',on);li.addEventListener('mouseleave',off);li.addEventListener('blur',off)});return o});

// paper plane: nose +Z, folded wings and keel
const V={N:[0,0,1.6],L:[1.15,.2,-1],R:[-1.15,.2,-1],C:[0,-.05,-1],B:[0,-.38,-.8]};
const tris=[['N','L','C',.98],['N','C','R',.84],['C','B','N',.6],['L','R','C',.7]];
const pp=[],pc=[];tris.forEach(([a,b,c,s])=>[a,b,c].forEach(k=>{pp.push(...V[k]);pc.push(s,s,s*1.02)}));
const pg=new T.BufferGeometry();pg.setAttribute('position',new T.Float32BufferAttribute(pp,3));pg.setAttribute('color',new T.Float32BufferAttribute(pc,3));pg.computeVertexNormals();
const plane=new T.Mesh(pg,M(0xffffff,{vertexColors:true,side:T.DoubleSide,roughness:.9,metalness:0}));plane.scale.setScalar(1.3);sc.add(plane);

// dust for depth and speed cues
const dp=[];for(let k=0;k<(small?250:600);k++){const c=curve.getPoint(Math.random());dp.push(c.x+(Math.random()-.5)*60,c.y+(Math.random()-.5)*40,c.z+(Math.random()-.5)*60)}
const dg=new T.BufferGeometry();dg.setAttribute('position',new T.Float32BufferAttribute(dp,3));
sc.add(new T.Points(dg,new T.PointsMaterial({size:.18,color:0x8fa3b8,transparent:true,opacity:.5})));

// scroll mapping: native scroll, piecewise ease so the plane dwells at each station
const all=[$('#intro'),...secs,$('#landing')];let anchors=[0,1];
const calc=()=>{const mx=Math.max(1,root.scrollHeight-innerHeight);anchors=all.map(e=>Math.min(1,Math.max(0,(e.offsetTop+e.offsetHeight/2-innerHeight/2)/mx)));anchors[0]=0;anchors[anchors.length-1]=1};
const map=f=>{let k=0;while(k<anchors.length-2&&f>anchors[k+1])k++;const u=Math.min(1,Math.max(0,(f-anchors[k])/Math.max(1e-6,anchors[k+1]-anchors[k]))),s=u*u*(3-2*u);return ts[k]+(ts[k+1]-ts[k])*s};
const resize=()=>{const W=innerWidth,H=innerHeight;r.setSize(W,H,false);cam.aspect=W/H;
  if(W>=900)cam.setViewOffset(W,H,W*.14,0,W,H);else cam.clearViewOffset();cam.updateProjectionMatrix();calc()};
addEventListener('resize',resize);resize();addEventListener('load',calc);

const links=[...document.querySelectorAll('nav a')],names=['DEPARTURE',...secs.map((s,i)=>'STN 0'+(i+1)+' / 0'+N+' · '+s.querySelector('h2').textContent.toUpperCase()),'ARRIVAL'];
let cur=0,bank=0,act=-1,tm=0,last=performance.now();
const look=new T.Vector3(),tg=new T.Vector3(),t1=new T.Vector3(),aim=new T.Vector3();
function frame(now){
  requestAnimationFrame(frame);
  const dt=Math.min(.05,(now-last)/1000);last=now;tm+=dt;
  const f=Math.min(1,Math.max(0,scrollY/Math.max(1,root.scrollHeight-innerHeight))),tgt=map(f);
  cur+=(tgt-cur)*(reduce?1:1-Math.pow(.0008,dt));
  const P=curve.getPoint(cur);curve.getTangent(Math.min(cur,.995),tg);curve.getTangent(Math.min(cur+.01,1),t1);
  const turn=tg.x*t1.z-tg.z*t1.x;bank+=(Math.max(-.7,Math.min(.7,turn*70))-bank)*.08;
  plane.position.copy(P);if(!reduce)plane.position.y+=Math.sin(tm*1.4)*.12;
  plane.lookAt(P.x+tg.x,P.y+tg.y,P.z+tg.z);plane.rotateZ(bank);
  const cp=curve.getPoint(Math.max(0,cur-.014));cam.position.set(cp.x,cp.y+3.2,cp.z);
  let w=0,c=null;for(let i=0;i<N;i++){const k=1-Math.min(1,Math.abs(cur-ts[i+1])/.07);if(k>w){w=k;c=S[i]}}
  aim.copy(curve.getPoint(Math.min(1,cur+.02)));if(c)aim.lerp(c,w*.85);
  look.lerp(aim,reduce?1:.1);cam.lookAt(look);
  st.forEach((o,i)=>o.mods.forEach((m,j)=>{const on=hi&&hi[0]===i&&hi[1]===j;m.em.emissiveIntensity+=((on?3:.5)-m.em.emissiveIntensity)*.15;m.g.scale.setScalar(m.g.scale.x+((on?1.12:1)-m.g.scale.x)*.15)}));
  r.render(sc,cam);
  let k=0;for(let q=1;q<anchors.length;q++)if(Math.abs(anchors[q]-f)<Math.abs(anchors[k]-f))k=q;
  bar.style.transform='scaleX('+f.toFixed(4)+')';
  if(k!==act){act=k;hud.textContent=names[k];links.forEach((l,i)=>i===k-1?l.setAttribute('aria-current','true'):l.removeAttribute('aria-current'));
    const s=secs[k-1];root.style.setProperty('--a',s?s.dataset.accent:'#e8a04a')}
}
requestAnimationFrame(frame);
})();
