package app.izhang.faciallogin;

import android.content.Context;

import java.io.File;

public class FileManager {

    public static String BASE_IMG_REF = "base_img";
    public static String COMPARE_IMG_REF = "compare_img";

    public static File getBaseImg(Context context){
        return new File(context.getFilesDir(), BASE_IMG_REF);
    }

    public static File getCompareImg(Context context){
        return new File(context.getFilesDir(), COMPARE_IMG_REF);
    }

}
