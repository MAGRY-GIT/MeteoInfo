package org.meteoinfo.map;

import org.meteoinfo.common.Extent;
import org.meteoinfo.data.GridData;
import org.meteoinfo.data.GridDataSetting;
import org.meteoinfo.data.StationData;
import org.meteoinfo.data.meteodata.MeteoDataInfo;
import org.meteoinfo.geo.analysis.InterpolationMethods;
import org.meteoinfo.geo.analysis.InterpolationSetting;
import org.meteoinfo.geo.layer.VectorLayer;
import org.meteoinfo.geo.layout.LayoutLegend;
import org.meteoinfo.geo.layout.LegendStyles;
import org.meteoinfo.geo.layout.MapLayout;
import org.meteoinfo.geo.legend.MapFrame;
import org.meteoinfo.geo.mapdata.MapDataManage;
import org.meteoinfo.geo.mapview.MapView;
import org.meteoinfo.geo.meteodata.DrawMeteoData;
import org.meteoinfo.geo.util.GeoMathUtil;
import org.meteoinfo.geometry.legend.LegendScheme;
import org.meteoinfo.geometry.legend.PolygonBreak;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.*;
import java.util.List;

class MeteoInfoMapTest6 {

    public static void main(String[] args) throws Exception {
        // 创建一个示例数据列表
        List<Map<String, Object>> list = new ArrayList<>();

        //
        // // 四川省的经纬度范围
        // double lonMin = 97.21;
        // double lonMax = 108.67;
        // double latMin = 26.03;
        // double latMax = 34.19;
        //
        // // 随机生成100条数据
        // Random random = new Random();
        // for (int i = 0; i < 100; i++) {
        //     String stationName = "Station" + (i + 1);
        //     double lon = lonMin + (lonMax - lonMin) * random.nextDouble(); // 在经度范围内随机生成
        //     double lat = latMin + (latMax - latMin) * random.nextDouble(); // 在纬度范围内随机生成
        //     double tem = 15 + random.nextDouble() * 20; // 生成15到35度之间的随机温度
        //
        //     addStationData(list, stationName, lon, lat, tem);
        // }


        // 添加示例站点数据
        // addStationData(list, "山西", 116.391453, 39.907103, 4);
        // addStationData(list, "北京", 116.4074, 39.9042, 25.0);
        // addStationData(list, "上海", 121.4737, 31.2304, 28.0);
        // addStationData(list, "广州", 113.2644, 23.1291, 30.0);
        addStationData(list, "成都", 104.0668, 30.6555, 22.0);
        addStationData(list, "西安", 108.9486, 34.2633, 20.0);
        // addStationData(list, "西安", 117.383719, 40.188195, -20.0);
        // addStationData(list, "西安", 117.383712, 40.188191, -10.0);
        // addStationData(list, "西安", 117.383713, 40.188192, 10.0);
        // addStationData(list, "西安", 117.383714, 40.188193, 20.0);
        // 创建站点格点
        StationData stationData = new StationData();
        // 循环数据将值塞入格点中
        for (Map<String, Object> item : list) {
            System.out.println(item);
            stationData.addData(String.valueOf(item.get("Station_Name")), Double.parseDouble(item.get("Lon").toString()), Double.parseDouble(item.get("Lat").toString()), Double.parseDouble(item.get("TEM").toString()));
        }
        // 读取地图图层
        VectorLayer altMap = MapDataManage.readMapFile_ShapeFile("C:\\Users\\Administrator\\Downloads\\成都市\\成都市.shp");

        // 创建网格设置参数
        GridDataSetting gridDataSetting = new GridDataSetting();
        // 设定数据区域
        gridDataSetting.dataExtent = altMap.getExtent();
        // 设定格点数
        gridDataSetting.xNum = 1000;
        gridDataSetting.yNum = 1000;

        // 创建插值设置
        InterpolationSetting interpolationSetting = new InterpolationSetting();
        // 设定格点配置
        interpolationSetting.setGridDataSetting(gridDataSetting);
        // 设定插值方法
        interpolationSetting.setInterpolationMethod(InterpolationMethods.KRIGING);
        // 设定搜索半径
        interpolationSetting.setRadius(10);
        // 设置最小点数
        interpolationSetting.setMinPointNum(1);
        // 插值到格点
        long x = System.currentTimeMillis();
        System.out.println(x);
        GridData gridData = GeoMathUtil.interpolateData(stationData, interpolationSetting);
        long y = System.currentTimeMillis();

        System.out.println(y-x);
        LegendScheme als = readFromLgs("C:\\Users\\Administrator\\Downloads\\色阶\\TEM.lgs");
        // 绘制图层
        VectorLayer layer = DrawMeteoData.createShadedLayer(gridData, "Data", "Data", true);
        // 创建视图
        MapView view = new MapView();
        PolygonBreak pb = (PolygonBreak) altMap.getLegendScheme().getLegendBreak(0);
        pb.setDrawFill(false);
        pb.setOutlineColor(Color.RED);
        layer = layer.clip(altMap);
        // 叠加图层
        view.addLayer(layer);
        view.addLayer(altMap);


        /**
         * 通用方法,可以抽成工具类
         */
        MapLayout layout = new MapLayout();
        layout.getActiveMapFrame().setMapView(view);
        view.setAntiAlias(true);
        layout.setAntiAlias(true);

        //根据视图计算视图的宽高
        Extent extent = view.getExtent();
        int size = 800;
        Rectangle rectangle = new Rectangle(size, (int) (size * 1D / extent.getWidth() * extent.getHeight()));

        //设置地图区域大小和外边距
        int width = rectangle.width;
        int left = 50;
        int height = rectangle.height;
        int right = 140;
        int top = 50;
        int bottom = 50;
        //设置页面边界
        layout.setPageBounds(new Rectangle(0, 0, width + left + right, height + top + bottom));
        //获取地图框
        MapFrame frame = layout.getActiveMapFrame();
        //设置布局边界
        frame.setLayoutBounds(new Rectangle(left, top, width, height));
        //绘制网格刻度线
        frame.setDrawGridLine(true);
        //设置网格间隔值
        frame.setGridXDelt(0.5);
        frame.setGridYDelt(0.5);
        //设置图例
        Rectangle bounds = layout.getActiveMapFrame().getLayoutBounds();
        LayoutLegend legend = layout.addLegend(bounds.x + bounds.width + 15, 0);
        legend.setLegendStyle(LegendStyles.NORMAL);
        legend.setTop(bounds.y + (bounds.height - legend.getHeight()) / 2);
        legend.setLegendLayer(layer);

        String imagePath = "C:\\Users\\Administrator\\Desktop\\6.png";

        layout.exportToPicture(imagePath);
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