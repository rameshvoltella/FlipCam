package com.flipcam.constants;

/**
 * Created by koushick on 22-Aug-17.
 */

public class Constants
{
    //Message to be sent to threads
    public final static int FRAME_AVAILABLE = 1000;
    public final static int RECORD_STOP = 2000;
    public final static int RECORD_START = 3000;
    public final static int RECORD_COMPLETE = 13000;
    public final static int SHUTDOWN = 6000;
    public final static int GET_CAMERA_RENDERER_INSTANCE = 8000;
    public final static int SHOW_MEMORY_CONSUMED = 5000;
    public final static int SHOW_ELAPSED_TIME = 10000;
    public final static int RECORD_STOP_ENABLE = 14000;

    //File size for calculating memory consumed
    public final static double KILO_BYTE = 1024.0;
    public final static double MEGA_BYTE = KILO_BYTE * KILO_BYTE;
    public final static double GIGA_BYTE = MEGA_BYTE * KILO_BYTE;

    //To fetch first frame to display thumbnail
    public static final long FIRST_SEC_MICRO = 1000000;
    //To update seek bar
    public static final int VIDEO_SEEK_UPDATE = 100;
    //Constants for saving media controls settings
    public static final String MEDIA_POSITION = "position";
    public static final String MEDIA_PLAYING = "playing";
    public static final String MEDIA_CONTROLS_HIDE = "mediaControlHide";
    public static final String IMAGE_CONTROLS_HIDE = "imageControlHide";
    public static final String SEEK_DURATION = "seekDuration";
    public static final String MEDIA_ACTUAL_DURATION = "mediaActualDuration";
    public static final String MEDIA_COMPLETED = "mediaCompleted";
    public static final String MEDIA_PREVIOUS_POSITION = "previousPos";
    public static final String MEDIA_CURRENT_TIME = "currentTime";
}
