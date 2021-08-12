package io.openchaos.generator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * 获取字符串MD5值
 *
 * @author tangbincheng
 *
 */
public class MD5{

    /**
     * 获取字符串的16位md5值
     * @param sourceStr
     * @return
     */
    public static String MD5_16(String sourceStr) {
        String md5 = MD5_32(sourceStr).substring(8, 24);
        return md5;
    }

    /**
     * 获取字符串的32位md5值
     * @param sourceStr
     * @return
     */
    public static String MD5_32(String sourceStr) {
        String result = "";
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(sourceStr.getBytes());
            byte b[] = md.digest();
            int i;
            StringBuffer buf = new StringBuffer("");
            for (int offset = 0; offset < b.length; offset++) {
                i = b[offset];
                if (i < 0)
                    i += 256;
                if (i < 16)
                    buf.append("0");
                buf.append(Integer.toHexString(i));
            }
            result = buf.toString();
        } catch (NoSuchAlgorithmException e) {
            System.out.println(e);
        }
        return result;
    }
//    public static void main(String[] args){
//        String str = MD5_16("abc");
//        System.out.println(str);
//    }
}