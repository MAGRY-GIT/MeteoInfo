package org.meteoinfo.map;

import com.github.weisj.jsvg.util.PathUtil;
import org.meteoinfo.common.Extent;
import org.meteoinfo.data.GridData;
import org.meteoinfo.data.GridDataSetting;
import org.meteoinfo.data.StationData;
import org.meteoinfo.data.meteodata.MeteoDataInfo;
import org.meteoinfo.geo.analysis.InterpolationMethods;
import org.meteoinfo.geo.analysis.InterpolationSetting;
import org.meteoinfo.geo.layer.VectorLayer;
import org.meteoinfo.geo.layout.MapLayout;
import org.meteoinfo.geo.mapdata.MapDataManage;
import org.meteoinfo.geo.mapview.MapView;
import org.meteoinfo.geo.meteodata.DrawMeteoData;
import org.meteoinfo.geo.util.GeoMathUtil;
import org.meteoinfo.geometry.legend.LegendScheme;
import org.meteoinfo.geometry.legend.PolygonBreak;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class MeteoInfoMapTest {

    public static void main(String[] args) throws Exception {

        // 从数据库查询cimiss数据
        List<Map<String, Object>> list = new ArrayList<>();
        // 添加示例站点数据
        addStationData(list, "Station1", 121.0, 31.0, 28.5);
        addStationData(list, "Station2", 121.5, 31.5, 30.0);
        addStationData(list, "Station3", 122.0, 32.0, 29.0);
        addStationData(list, "Station4", 122.5, 32.5, 27.5);
        addStationData(list, "Station5", 123.0, 33.0, 26.0);
        // 创建站点格点
        StationData stationData = new StationData();
        // 循环数据将值塞入格点中
        for (Map<String, Object> item : list) {
            System.out.println(item);
            stationData.addData(String.valueOf(item.get("Station_Name")), Double.parseDouble(item.get("Lon").toString()), Double.parseDouble(item.get("Lat").toString()), Double.parseDouble(item.get("TEM").toString()));
        }
        // 读取地图图层
        VectorLayer altMap = MapDataManage.readMapFile_ShapeFile("C:\\Users\\Administrator\\Downloads\\四川省\\四川省.shp");
        // 创建网格设置参数
        GridDataSetting gridDataSetting = new GridDataSetting();
        // 设定数据区域
        gridDataSetting.dataExtent = altMap.getExtent();
        // 设定格点数
        gridDataSetting.xNum = list.size();
        gridDataSetting.yNum = list.size();
        // 创建插值设置
        InterpolationSetting interpolationSetting = new InterpolationSetting();
        // 设定格点配置
        interpolationSetting.setGridDataSetting(gridDataSetting);
        // 设定插值方法
        interpolationSetting.setInterpolationMethod(InterpolationMethods.IDW_RADIUS);
        // 设定搜索半径
        interpolationSetting.setRadius(10);
        // 设置最小点数
        interpolationSetting.setMinPointNum(1);
        // 插值到格点
        GridData gridData = GeoMathUtil.interpolateData(stationData, interpolationSetting);

        LegendScheme als = readFromLgs("C:\\Users\\Administrator\\Downloads\\色阶\\TEM.lgs");
        // 绘制图层
        VectorLayer layer = DrawMeteoData.createShadedLayer(gridData, als, "", "", true);
        // 创建视图
        MapView view = new MapView();
        PolygonBreak pb = (PolygonBreak) altMap.getLegendScheme().getLegendBreak(0);
        pb.setDrawFill(false);
        pb.setOutlineColor(Color.GRAY);
        layer = layer.clip(altMap);
        // 叠加图层
        view.addLayer(layer);
        view.addLayer(altMap);


        /**
         * 通用方法,可以抽成工具类
         */
        MapLayout layout = new MapLayout();
        // 去除图形边框
        layout.getActiveMapFrame().setDrawNeatLine(false);
        // 区域边界
        Extent extent = view.getExtent();
        // 设置矩形的宽和高
        Rectangle bounds = new Rectangle(800, (int) (800 * 1D / extent.getWidth() * extent.getHeight()));
        // 设置地图边框
        layout.setPageBounds(new Rectangle(0, 0, bounds.width, bounds.height));
        // 设置页面边框
        layout.getActiveMapFrame().setLayoutBounds(new Rectangle(0, 0, bounds.width, bounds.height));
        layout.getActiveMapFrame().setMapView(view);
        layout.exportToPicture("C:\\Users\\Administrator\\Desktop\\1.png");
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