package org.meteoinfo.map;

import org.meteoinfo.common.Extent;
import org.meteoinfo.data.GridData;
import org.meteoinfo.data.StationData;
import org.meteoinfo.data.meteodata.MeteoDataInfo;
import org.meteoinfo.data.meteodata.StationModelData;
import org.meteoinfo.geo.layer.RasterLayer;
import org.meteoinfo.geo.layer.VectorLayer;
import org.meteoinfo.geo.layout.MapLayout;
import org.meteoinfo.geo.mapdata.MapDataManage;
import org.meteoinfo.geo.mapview.MapView;
import org.meteoinfo.geo.meteodata.DrawMeteoData;
import org.meteoinfo.geometry.legend.LegendScheme;
import org.meteoinfo.geometry.legend.PolygonBreak;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class MeteoInfoMapTest3 {

    public static void main(String[] args) throws Exception {
        MeteoDataInfo meteoDataInfo = new MeteoDataInfo();
        String path = "C:\\Users\\Administrator\\Downloads\\Z_NAFP_C_BABJ_20250303002848_P_CLDAS_RT_ASI_0P0625_DAY-TMP-2025030200.nc";
        meteoDataInfo.openNetCDFData(path);
        meteoDataInfo.setVariableName("TAIR_DAY_MAX");
//读取地图A
        VectorLayer scmap = MapDataManage.readMapFile_ShapeFile("C:\\Users\\Administrator\\Downloads\\中华人民共和国\\中华人民共和国.shp");
//描述地图边界线
        PolygonBreak pb = (PolygonBreak) scmap.getLegendScheme().getLegendBreak(0);
//是否设置填充
        pb.setDrawFill(false);
//设置轮廓大小
        pb.setOutlineSize(2f);
//设置轮廓颜色
        pb.setOutlineColor(Color.black);
//读取色阶
        LegendScheme als = readFromLgs("C:\\Users\\Administrator\\Downloads\\色阶\\TEM.lgs");
//绘制图层
         VectorLayer layer = DrawMeteoData.createWeatherSymbolLayer(meteoDataInfo.getStationData(),"1","2");
//创建视图
        MapView view = new MapView();
//叠加图层
//         view.addLayer(layer);
        view.addLayer(scmap);
        MapLayout layout  = new MapLayout();
//去除图形边框
        layout.getActiveMapFrame().setDrawNeatLine(false);
//区域边界
        Extent extent = view.getExtent();
//设置矩形的宽和高
        Rectangle bounds = new Rectangle(800, (int) (800 * 1D / extent.getWidth() * extent.getHeight()));
//设置地图边框
        layout.setPageBounds(new Rectangle(0, 0, bounds.width, bounds.height));
//设置页面边框
        layout.getActiveMapFrame().setLayoutBounds(new Rectangle(0, 0, bounds.width, bounds.height));
        layout.getActiveMapFrame().setMapView(view);
        String imagePath = "C:\\Users\\Administrator\\Desktop\\3.png";
        layout.exportToPicture(imagePath);



        //透明处理
        //读取图片
        BufferedImage bi = ImageIO.read(new File(imagePath));
        //类型转换
        BufferedImage img = new BufferedImage(bi.getWidth(), bi.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = (Graphics2D) img.getGraphics();
        g.drawImage(bi, null, 0, 0);
        //透明处理
        int alpha = 0;
        for(int i=img.getMinY(); i<img.getHeight(); i++){
            for(int j=img.getMinX(); j<img.getWidth(); j++){
                int rgb = img.getRGB(j, i);
                //透明部分不需要处理
                if(rgb < 0){
                    int R = (rgb & 0xff0000) >> 16;
                    int G = (rgb & 0xff00) >> 8;
                    int B = (rgb & 0xff);
                    //将白色剔除
                    Color color = Color.white;
                    if(color.getRed() == R && color.getGreen() == G && color.getBlue() == B){
                        alpha = 0;
                    }
                    else {
                        alpha = 255;
                    }
                    rgb = (alpha << 24) | (rgb & 0x00ffffff);
                    img.setRGB(j, i, rgb);
                }
            }
        }
        //释放资源
        g.dispose();
        ImageIO.write(img, "png", new File(imagePath));
    }

    public static LegendScheme readFromLgs(String path) throws Exception {
        LegendScheme scheme = new LegendScheme();
        scheme.importFromXMLFile(path, false);
        return scheme;
    }

    // 辅助方法：添加站点数据到列表
    private static void addStationData(List<Map<String, Object>> list, String stationName, double lon, double lat, double tem) {
        Map<String, Object> item = new HashMap<>();
        item.put("Station_Name", stationName);
        item.put("Lon", lon);
        item.put("Lat", lat);
        item.put("TEM", tem);
        list.add(item);
    }
}