package org.meteoinfo.map;

import org.meteoinfo.common.Extent;
import org.meteoinfo.data.GridData;
import org.meteoinfo.data.meteodata.MeteoDataInfo;
import org.meteoinfo.geo.layer.RasterLayer;
import org.meteoinfo.geo.layout.MapLayout;
import org.meteoinfo.geo.mapview.MapView;
import org.meteoinfo.geo.meteodata.DrawMeteoData;
import org.meteoinfo.geometry.legend.LegendScheme;
import org.meteoinfo.ndarray.Array;
import org.meteoinfo.ndarray.Index;

import java.awt.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class MeteoInfoMapTest5 {

    /**
     * 主程序入口
     * 本程序主要用于处理气象数据，并将其可视化为地图图像
     * 具体步骤包括：读取NetCDF气象数据文件，创建栅格数据图层，设置地图视图，以及导出可视化图像
     */
    public static void main(String[] args) throws Exception {
        // 指定NetCDF文件路径
        String path = "C:\\Users\\Administrator\\Downloads\\Z_NAFP_C_BABJ_20250303002848_P_CLDAS_RT_ASI_0P0625_DAY-TMP-2025030200.nc";

        // 创建MeteoDataInfo对象以处理气象数据
        MeteoDataInfo meteo = new MeteoDataInfo();

        // 打开NetCDF数据文件
        meteo.openNetCDFData(path);
        // 设置需要处理的气象变量名称
        String tairDayMax = "TAIR_DAY_MAX";
        meteo.setVariableName(tairDayMax);
        // 创建栅格数据图层
        GridData gridData = meteo.getGridData();
        RasterLayer layer = DrawMeteoData.createRasterLayer(gridData, "");
        // 初始化地图视图
        MapView view = new MapView();
        // 将栅格数据图层添加到地图视图中
        view.addLayer(layer);
        ;

        /*
         * 以下为通用方法,可以抽成工具类
         * 这部分代码用于设置地图布局和导出地图图像
         */
        MapLayout layout  = new MapLayout();
        //去除图形边框
        layout.getActiveMapFrame().setDrawNeatLine(false);
        //区域边界
        Extent extent = view.getExtent();
        //设置矩形的宽和高
        Rectangle bounds = new Rectangle(800, (int) (800 * 1D / extent.getWidth() * extent.getHeight()));
        //设置地图边框(分辨率)
        layout.setPageBounds(new Rectangle(0, 0, bounds.width, bounds.height));
        //设置页面边框(控制页面平铺)
        layout.getActiveMapFrame().setLayoutBounds(new Rectangle(0, 0, bounds.width, bounds.height));
        //将地图视图设置到活动地图框架中()
        layout.getActiveMapFrame().setMapView(view);
      String name = tairDayMax +","+  gridData.getBorderYMax() + "-"+  gridData.getBorderXMax() + "," +  gridData.getBorderYMin()+"-"+  gridData.getBorderXMin();
        //指定导出图像的路径
        String imagePath = "C:\\Users\\Administrator\\Desktop\\" + name+".png";

        //将地图布局导出为图像文件
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