/* Original no-content QA: exercise libretro initialization/teardown without game or BIOS. */
#include <dlfcn.h>
#include <stdbool.h>
#include <stdint.h>
#include <stdio.h>
#include <stdarg.h>
#include <string.h>
#include "libretro.h"
static const char *core_path;
static void log_cb(enum retro_log_level level, const char *fmt, ...) {
    (void)level; va_list args; va_start(args,fmt); vfprintf(stderr,fmt,args); va_end(args);
}
static bool env(unsigned cmd, void *data) {
    switch (cmd) {
      case RETRO_ENVIRONMENT_GET_SYSTEM_DIRECTORY:
      case RETRO_ENVIRONMENT_GET_SAVE_DIRECTORY: *(const char **)data="/data/local/tmp/emuui-core-lifecycle"; return true;
      case RETRO_ENVIRONMENT_GET_LIBRETRO_PATH: *(const char **)data=core_path; return true;
      case RETRO_ENVIRONMENT_GET_LOG_INTERFACE: ((struct retro_log_callback *)data)->log=log_cb; return true;
      case RETRO_ENVIRONMENT_GET_CAN_DUPE: *(bool *)data=true; return true;
      case RETRO_ENVIRONMENT_GET_VARIABLE_UPDATE: *(bool *)data=false; return true;
      case RETRO_ENVIRONMENT_GET_VARIABLE: ((struct retro_variable *)data)->value=NULL; return false;
      case RETRO_ENVIRONMENT_GET_LANGUAGE: *(unsigned *)data=RETRO_LANGUAGE_ENGLISH; return true;
      case RETRO_ENVIRONMENT_SET_VARIABLES:
      case RETRO_ENVIRONMENT_SET_SUPPORT_NO_GAME:
      case RETRO_ENVIRONMENT_SET_PIXEL_FORMAT:
      case RETRO_ENVIRONMENT_SET_INPUT_DESCRIPTORS: return true;
      default: return false;
    }
}
static void video(const void *d,unsigned w,unsigned h,size_t p){(void)d;(void)w;(void)h;(void)p;}
static void audio(int16_t l,int16_t r){(void)l;(void)r;}
static size_t batch(const int16_t *d,size_t n){(void)d;return n;}
static void poll(void){}
static int16_t input(unsigned p,unsigned d,unsigned i,unsigned id){(void)p;(void)d;(void)i;(void)id;return 0;}
#define CALL_SET(name,type,value) do { void (*f)(type)=dlsym(core,name); if(!f)return 3;f(value); } while(0)
int main(int argc,char **argv) {
    if(argc!=2)return 2;core_path=argv[1];void *core=dlopen(core_path,RTLD_NOW|RTLD_LOCAL);
    if(!core){fprintf(stderr,"%s\n",dlerror());return 1;}
    CALL_SET("retro_set_environment",retro_environment_t,env);
    CALL_SET("retro_set_video_refresh",retro_video_refresh_t,video);
    CALL_SET("retro_set_audio_sample",retro_audio_sample_t,audio);
    CALL_SET("retro_set_audio_sample_batch",retro_audio_sample_batch_t,batch);
    CALL_SET("retro_set_input_poll",retro_input_poll_t,poll);
    CALL_SET("retro_set_input_state",retro_input_state_t,input);
    void (*init)(void)=dlsym(core,"retro_init"),(*deinit)(void)=dlsym(core,"retro_deinit");
    if(!init||!deinit)return 3;
    for(int i=0;i<2;i++){init();deinit();printf("PASS init/deinit cycle %d; no game or BIOS loaded\n",i+1);fflush(stdout);}
    return 0;
}
