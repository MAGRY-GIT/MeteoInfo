package org.meteoinfo.map;

import org.meteoinfo.common.Extent;
import org.meteoinfo.data.GridData;
import org.meteoinfo.data.GridDataSetting;
import org.meteoinfo.data.StationData;
import org.meteoinfo.data.meteodata.MeteoDataInfo;
import org.meteoinfo.geo.analysis.InterpolationMethods;
import org.meteoinfo.geo.analysis.InterpolationSetting;
import org.meteoinfo.geo.layer.RasterLayer;
import org.meteoinfo.geo.layer.VectorLayer;
import org.meteoinfo.geo.layout.MapLayout;
import org.meteoinfo.geo.mapdata.MapDataManage;
import org.meteoinfo.geo.mapview.MapView;
import org.meteoinfo.geo.meteodata.DrawMeteoData;
import org.meteoinfo.geo.util.GeoMathUtil;
import org.meteoinfo.geometry.legend.LegendScheme;
import org.meteoinfo.geometry.legend.PolygonBreak;
import org.meteoinfo.ndarray.Array;
import org.meteoinfo.ndarray.Index;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class MeteoInfoMapTest7 {

    public static void main(String[] args) throws Exception {

        String path = "C:\\Users\\Administrator\\Downloads\\FY4A-_AGRI--_N_DISK_1047E_L2-_CTT-_MULT_NOM_20190209140000_20190209141459_4000M_V0001.nc";
        MeteoDataInfo meteo = new MeteoDataInfo();
        meteo.openNetCDFData(path);
        //行列位置读取
        Array column = meteo.read("x");
        //数据
        Array array = meteo.read("y");
        //获取x,y下标数
        int[] shape = array.getShape();
        //获取x起点,y起点
        Index index = array.getIndex();
        GridData grid = new GridData(0, 0.1, shape[1], 0, 0.1, shape[0]);
        grid.setMissingValue(-1);
        //第一有效值,最后有效值,平分除二
        for(int i=0; i<column.getSize()/2; i++){
            //2字节,第一个值
            int x = column.getShort(i * 2);
            //2字节,最后一个值
            int y = column.getShort(i * 2 + 1);
            if(x != -1 && y != -1){
                for(int j=x; j<=y; j++){
                    index.set(i, j);
                    grid.getData()[shape[0] - i - 1][j] = array.getShort(index);
                }
            }
        }
        RasterLayer layer = DrawMeteoData.createRasterLayer(grid, "");
        MapView view = new MapView();
        view.addLayer(layer);

        /**
         * 通用方法,可以抽成工具类
         */
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
        String imagePath = "C:\\Users\\Administrator\\Desktop\\7.png";

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