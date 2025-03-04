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

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class MeteoInfoMapTest2 {

    public static void main(String[] args) throws Exception {
        MeteoDataInfo meteo = new MeteoDataInfo();
        meteo.openAWXData("C:\\Users\\Administrator\\Downloads\\nmc_met_io-master\\nmc_met_io-master\\examples\\samples\\ANI_IR1_R04_20220331_2100_FY2G.AWX");
        GridData grid = meteo.getGridData();
        // createRasterLayer：栅格图
        // createContourLayer：等值线图
        // createShadedLayer：等值面图（色斑图）
        // createGridBarbLayer：风羽图（风场图）
        // createStreamlineLayer：流场图
        //绘制图层
        //色阶文件ALT色阶
        String colorPath = "C:\\Users\\Administrator\\Downloads\\色阶\\AWX.pal";
        RasterLayer layer = DrawMeteoData.createRasterLayer(grid, "123",colorPath);

        //读取地图
        VectorLayer xzmap = MapDataManage.readMapFile_ShapeFile("C:\\Users\\Administrator\\Downloads\\中华人民共和国\\中华人民共和国.shp");

        //描述地图边界线
        PolygonBreak pb = (PolygonBreak) xzmap.getLegendScheme().getLegendBreak(0);
        //是否设置填充
        pb.setDrawFill(false);
        //设置轮廓大小
//        pb.setOutlineSize(2f);
        //设置轮廓颜色
        pb.setOutlineColor(Color.white);


        //创建视图
        MapView view = new MapView();
        //叠加图层
        view.addLayer(layer);
        view.addLayer(xzmap);


        MapLayout layout  = new MapLayout();
        //______去边框
        Extent extent = xzmap.getExtent();
        //设置矩形的宽和高
        Rectangle bounds = new Rectangle(800, (int) (800 * 1D / extent.getWidth() * extent.getHeight()));
        //设置地图边框
        layout.setPageBounds(new Rectangle(0, 0, bounds.width+100, bounds.height+100));
        //设置页面边框
        layout.getActiveMapFrame().setLayoutBounds(new Rectangle(50, 50, bounds.width, bounds.height));
        //设置缩放程度
        view.zoomToExtent(extent);
        //设置所有图层范围
        view.setExtent(extent);
        layout.getActiveMapFrame().setDrawGridLine(true);


        layout.getActiveMapFrame().setMapView(view);

        //绘制
        layout.exportToPicture("C:\\Users\\Administrator\\Desktop\\3.png");
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