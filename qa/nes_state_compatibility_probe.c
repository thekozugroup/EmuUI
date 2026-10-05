/* Original no-content QA: exercise libretro initialization/teardown without game or BIOS. */
#include <dlfcn.h>
#include <stdbool.h>
#include <stdint.h>
#include <stdio.h>
#include <stdarg.h>
#include <string.h>
#include <stdlib.h>
#include "libretro.h"
static const char *core_path;
static unsigned frame, pixels=2, video_calls;
static unsigned long long video_hash=1469598103934665603ULL;
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
      case RETRO_ENVIRONMENT_SET_PIXEL_FORMAT: pixels=(*(unsigned*)data==RETRO_PIXEL_FORMAT_XRGB8888)?4:2; return true;
      case RETRO_ENVIRONMENT_GET_VARIABLE_UPDATE: *(bool *)data=false; return true;
      case RETRO_ENVIRONMENT_GET_VARIABLE: ((struct retro_variable *)data)->value=NULL; return false;
      case RETRO_ENVIRONMENT_GET_LANGUAGE: *(unsigned *)data=RETRO_LANGUAGE_ENGLISH; return true;
      case RETRO_ENVIRONMENT_SET_VARIABLES:
      case RETRO_ENVIRONMENT_SET_SUPPORT_NO_GAME:
      case RETRO_ENVIRONMENT_SET_INPUT_DESCRIPTORS: return true;
      default: return false;
    }
}
static void video(const void *d,unsigned w,unsigned h,size_t pitch){if(!d)return;video_calls++;for(unsigned y=0;y<h;y++)for(unsigned x=0;x<w*pixels;x++){video_hash^=((const unsigned char*)d)[y*pitch+x];video_hash*=1099511628211ULL;}}
static void audio(int16_t l,int16_t r){(void)l;(void)r;}
static size_t batch(const int16_t *d,size_t n){(void)d;return n;}
static void poll(void){}
static int16_t input(unsigned p,unsigned d,unsigned i,unsigned id){(void)i;if(p||d!=RETRO_DEVICE_JOYPAD)return 0;return (id==RETRO_DEVICE_ID_JOYPAD_RIGHT && frame<30)||(id==RETRO_DEVICE_ID_JOYPAD_A && frame<15);}
#define CALL_SET(name,type,value) do { void (*f)(type)=dlsym(core,name); if(!f)return 3;f(value); } while(0)

static void *read_file(const char *path,size_t *size){FILE*f=fopen(path,"rb");if(!f)return NULL;fseek(f,0,SEEK_END);*size=ftell(f);rewind(f);void*b=malloc(*size);if(fread(b,1,*size,f)!=*size)exit(20);fclose(f);return b;}
static void write_file(const char *path,const void *data,size_t size){FILE*f=fopen(path,"wb");if(!f||fwrite(data,1,size,f)!=size)exit(21);fclose(f);}
int main(int argc,char **argv) {
 if(argc!=5)return 2; core_path=argv[1];void*core=dlopen(core_path,RTLD_NOW|RTLD_LOCAL);if(!core){fprintf(stderr,"%s\n",dlerror());return 1;}
 CALL_SET("retro_set_environment",retro_environment_t,env);CALL_SET("retro_set_video_refresh",retro_video_refresh_t,video);CALL_SET("retro_set_audio_sample",retro_audio_sample_t,audio);CALL_SET("retro_set_audio_sample_batch",retro_audio_sample_batch_t,batch);CALL_SET("retro_set_input_poll",retro_input_poll_t,poll);CALL_SET("retro_set_input_state",retro_input_state_t,input);
 void(*init)(void)=dlsym(core,"retro_init"),(*deinit)(void)=dlsym(core,"retro_deinit"),(*run)(void)=dlsym(core,"retro_run"),(*unload)(void)=dlsym(core,"retro_unload_game");
 bool(*load)(const struct retro_game_info*)=dlsym(core,"retro_load_game"),(*serialize)(void*,size_t)=dlsym(core,"retro_serialize"),(*unserialize)(const void*,size_t)=dlsym(core,"retro_unserialize");size_t(*serial_size)(void)=dlsym(core,"retro_serialize_size"),(*memory_size)(unsigned)=dlsym(core,"retro_get_memory_size");void*(*memory_data)(unsigned)=dlsym(core,"retro_get_memory_data");
 if(!init||!deinit||!run||!unload||!load||!serialize||!unserialize||!serial_size||!memory_size||!memory_data)return 3;
 size_t rom_size;void*rom=read_file(argv[2],&rom_size);if(!rom)return 4;struct retro_game_info game={argv[2],rom,rom_size,NULL};init();if(!load(&game)){fprintf(stderr,"LOAD GAME FAILED\n");return 5;}
 char ram_path[1024];snprintf(ram_path,sizeof(ram_path),"%s.srm",argv[4]);size_t ram_size=memory_size(RETRO_MEMORY_SAVE_RAM);unsigned char*ram=memory_data(RETRO_MEMORY_SAVE_RAM);
 if(strcmp(argv[3],"save")==0){
  for(frame=0;frame<120;frame++)run();
  if(!ram||ram_size==0){fprintf(stderr,"NO BATTERY RAM\n");return 6;}
  for(size_t i=0;i<ram_size;i++)ram[i]=(unsigned char)(i*37+11);write_file(ram_path,ram,ram_size);
  size_t n=serial_size();void*state=calloc(1,n);if(!serialize(state,n))return 7;write_file(argv[4],state,n);free(state);printf("SAVED state=%zu battery=%zu\n",n,ram_size);
 }else{
  size_t n;void*state=read_file(argv[4],&n);if(!state||!unserialize(state,n)){fprintf(stderr,"STATE REJECTED\n");return 8;}free(state);
  size_t expected_size;void*expected=read_file(ram_path,&expected_size);if(!expected||ram_size!=expected_size||!ram||memcmp(ram,expected,ram_size)){fprintf(stderr,"STATE BATTERY RAM MISMATCH\n");return 9;}free(expected);
  // Exercise the frontend's battery-file restoration contract independently.
  memset(ram,0,ram_size);expected=read_file(ram_path,&expected_size);memcpy(ram,expected,ram_size);free(expected);
  for(size_t i=0;i<ram_size;i++)if(ram[i]!=(unsigned char)(i*37+11))return 10;
  printf("RESTORED state=%zu battery=%zu\n",n,ram_size);
 }
 video_hash=1469598103934665603ULL;video_calls=0;for(frame=0;frame<60;frame++)run();if(video_calls<50)return 11;printf("FRAMES=%u HASH=%016llx\n",video_calls,video_hash);unload();deinit();free(rom);return 0;
}
