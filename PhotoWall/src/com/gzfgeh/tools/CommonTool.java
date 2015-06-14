package com.gzfgeh.tools;

import java.io.File;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Environment;

public class CommonTool {
	
	/** 
     * 根据传入的uniqueName获取硬盘缓存的路径地址。 
     */
	public static File getDiskCacheDir(Context context, String uniqueName) {  
        String cachePath;  
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())  
                || !Environment.isExternalStorageRemovable()) {  
            cachePath = context.getExternalCacheDir().getPath();  
        } else {  
            cachePath = context.getCacheDir().getPath();  
        }  
        return new File(cachePath + File.separator + uniqueName);  
    }
	
	/** 
     * 获取当前应用程序的版本号。 
     */  
    public static int getAppVersion(Context context) {  
        try {  
            PackageInfo info = context.getPackageManager().getPackageInfo(context.getPackageName(),  
                    0);  
            return info.versionCode;  
        } catch (NameNotFoundException e) {  
            e.printStackTrace();  
        }  
        return 1;  
    }
    
}
