package io.taucoin.torrent.publishing.core.utils.dimens;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * 各屏幕尺寸大小适配，执行main函数生成.xml文件
 */
public class DimenTool {

    /**
     * 根据UI画布大小比例进行换算，假设UI图分辨率为1080x1920
     * 高分率缩放因子密度一般为 density = 480 / 160 , 即 density = 3
     * 宽度 width = 1080
     * 可选项，根据你实际的UI设计图来定义
     */
    private static void gen() {
        // 密度因子
        float density = 2.8f;
        // 屏幕相对宽度
        int width = (int)(1080 / density);
        // 执行生成适配的dimens.xml文件
        gen(width);
    }

    /**
     * 生成对应的适配的dimens.xml文件
     * @param width
     */
    private static void gen(int width) {

        String fileDir = "./taucoin-android/src/main/res/";
        File file = new File(fileDir + "/values/dimens.xml");
        BufferedReader reader = null;
        Map<Integer, StringBuilder> map = new HashMap<>();
//        map.put(180, new StringBuilder());
//        map.put(240, new StringBuilder());
        map.put(320, new StringBuilder());
        map.put(360, new StringBuilder());
        map.put(380, new StringBuilder());
        map.put(410, new StringBuilder());
        map.put(480, new StringBuilder());
        map.put(540, new StringBuilder());
//        map.put(600, new StringBuilder());
//        map.put(720, new StringBuilder());
//        map.put(760, new StringBuilder());
//        map.put(800, new StringBuilder());
//        map.put(1080, new StringBuilder());
//        map.put(900, new StringBuilder());
//        map.put(1200, new StringBuilder());
//        map.put(1440, new StringBuilder());

        try {
            reader = new BufferedReader(new FileReader(file));
            String tempString;
            int toolbar_max_dp = 65;
            while ((tempString = reader.readLine()) != null) {

                if (tempString.contains("</dimen>")) {
                    //tempString = tempString.replaceAll(" ", "");
                    String start = tempString.substring(0, tempString.indexOf(">") + 1);
                    String end = tempString.substring(tempString.lastIndexOf("<") - 2);

                    int startIndex = tempString.indexOf(">") + 1;
                    int endIndex = tempString.indexOf("</dimen>") - 2;
                    String numStr = tempString.substring(startIndex, endIndex);
                    float num = Float.parseFloat(numStr);

                    boolean isToolbar = tempString.contains("toolbar_height") || tempString.contains("toolbar_title_text_size");
                    Set<Integer> keys = map.keySet();
                    for (Integer key: keys) {
                        StringBuilder sw = map.get(key);
                        int dpValue = Math.round(num * key / width);
                        if (isToolbar) {
                            dpValue = Math.min(dpValue, toolbar_max_dp);
                        }
                        if (null == sw) {
                            continue;
                        }
                        sw.append(start).append(dpValue).append(end).append("\n");
                    }
                } else {
                    Set<Integer> keys = map.keySet();
                    for (Integer key: keys) {
                        StringBuilder sw = map.get(key);
                        if (null == sw) {
                            continue;
                        }
                        sw.append(tempString).append("\n");
                    }
                }
            }
            reader.close();

            String swFile = fileDir + "values-sw%ddp/dimens.xml";
            Set<Integer> keys = map.keySet();
            for (Integer key: keys) {
                StringBuilder sw = map.get(key);
                if (null == sw) {
                    continue;
                }
                writeFile(String.format(swFile, key), sw.toString());
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        }
    }

    private static void writeFile(String file, String text) throws IOException {
        CreateFileUtil.createFile(file);
        PrintWriter out = null;
        FileWriter fileWriter = null;
        try {
            fileWriter = new FileWriter(file);
            out = new PrintWriter(new BufferedWriter(fileWriter));
            out.println(text);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (out != null) {
                out.close();
            }
            if (fileWriter != null) {
                fileWriter.close();
            }
        }
    }

    /**
     * 生成最基本的尺寸
     */
    private static void genBaseDimensSize() {
        System.out.println("<!--   ******************************Font******************************* -->");
        for (int i = 6; i <= 36; i++) {
            StringBuilder sb = new StringBuilder("<dimen name=\"font_size_");
            sb.append(i).append("\">").append(i).append("dp</dimen>");
            System.out.println(sb.toString());
        }
        System.out.println("<!--   ******************************Widget******************************* -->");
        for (int i = 1; i < 600; i++) {
            StringBuilder sb = new StringBuilder("<dimen name=\"widget_size_");
            if (i > 360) {
                i += 4;
                sb.append(i).append("\">").append(i).append("dp</dimen>");
            } else {
                sb.append(i).append("\">").append(i).append("dp</dimen>");
            }
            System.out.println(sb.toString());
        }
    }

    public static void main(String[] args) {
        // 生成最基本的尺寸, 可做参考
//        genBaseDimensSize();
        gen();
    }
}