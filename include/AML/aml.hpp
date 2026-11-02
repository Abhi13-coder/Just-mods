#pragma once
#include <stdint.h>
#include <stddef.h>

#ifdef __cplusplus
extern "C" {
#endif

typedef void* (*AML_GetSym_t)(void* handle, const char* sym);

struct AMLInterface {
    void* (*GetSym)(void* handle, const char* sym);
    void  (*Redirect)(uintptr_t from, uintptr_t to);
    void  (*PlaceNOP)(uintptr_t at, size_t count);
    void  (*PlaceJMP)(uintptr_t at, uintptr_t to);
    void* (*GetLib)(const char* name);
    uintptr_t (*GetLibAddr)(const char* name);
};

extern struct AMLInterface* aml;

#define DECL_HOOKv(name, ...) \
    void (*name##_orig)(__VA_ARGS__) = nullptr; \
    void name##_hook(__VA_ARGS__)

#define DECL_HOOK(ret, name, ...) \
    ret (*name##_orig)(__VA_ARGS__) = nullptr; \
    ret name##_hook(__VA_ARGS__)

#define HOOK(name, addr) \
    aml->Redirect((uintptr_t)(addr), (uintptr_t)name##_hook)

#ifdef __cplusplus
}
#endif
