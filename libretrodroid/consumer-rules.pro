# Native callbacks are looked up by name from the original JNI bridge.
-keepclassmembers class com.swordfish.libretrodroid.GLRetroView {
    private void refreshAspectRatio();
    private void sendRumbleEvent(int,float,float);
}
