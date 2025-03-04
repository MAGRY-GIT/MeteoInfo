package org.meteoinfo.image;

import org.apache.commons.imaging.ImageReadException;
import org.meteoinfo.ndarray.Array;

import java.io.IOException;

class ImageUtilTest {

    public static void main(String[] args) {
          String imagePath = "E:\\项目管理\\WEB视频基础平台\\1km_CMPAS_CHNPre01_china_20250226020000.png"; // 替换为你的图像路径

        try {
            Array array = ImageUtil.imageRead(imagePath);
            System.out.println(1);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (ImageReadException e) {
            throw new RuntimeException(e);
        }
    }

    void imageRead() {
    }
}