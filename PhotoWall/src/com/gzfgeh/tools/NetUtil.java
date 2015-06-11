package com.gzfgeh.tools;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class NetUtil {
	
	public static byte[] read(InputStream in) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int len;
        while ((len = in.read(buffer)) != -1)
            baos.write(buffer, 0, len);
        baos.close();
        byte[] data = baos.toByteArray();
        return data;
    }
}
