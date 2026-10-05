/* Original QA utility: load a core and read its public ABI metadata, without a ROM or BIOS. */
#include <dlfcn.h>
#include <stdbool.h>
#include <stdio.h>
#include <string.h>

struct retro_system_info {
    const char *library_name;
    const char *library_version;
    const char *valid_extensions;
    bool need_fullpath;
    bool block_extract;
};

int main(int argc, char **argv) {
    if (argc != 2 && !(argc == 3 && strcmp(argv[2], "--load-only") == 0)) return 2;
    void *core = dlopen(argv[1], RTLD_NOW | RTLD_LOCAL);
    if (!core) {
        fprintf(stderr, "LOAD FAILED: %s\n", dlerror());
        return 1;
    }
    if (argc == 3) {
        puts("LOAD PASS (dlopen only; no JNI initialization or gameplay)");
        return 0;
    }
    void (*info_fn)(struct retro_system_info *) = dlsym(core, "retro_get_system_info");
    unsigned (*api_fn)(void) = dlsym(core, "retro_api_version");
    if (!info_fn || !api_fn) return 3;
    struct retro_system_info info = {0};
    info_fn(&info);
    printf("LOAD PASS api=%u name=%s version=%s\n", api_fn(),
           info.library_name ? info.library_name : "(null)",
           info.library_version ? info.library_version : "(null)");
    /* Process exit isolates core teardown and globals; no retro_init/run/unload calls. */
    return 0;
}
