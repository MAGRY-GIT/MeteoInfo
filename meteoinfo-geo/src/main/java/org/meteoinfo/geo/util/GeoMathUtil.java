package org.meteoinfo.geo.util;

import org.locationtech.proj4j.datum.Grid;
import org.meteoinfo.common.PointD;
import org.meteoinfo.data.GridData;
import org.meteoinfo.data.GridDataSetting;
import org.meteoinfo.data.StationData;
import org.meteoinfo.geo.layer.VectorLayer;
import org.meteoinfo.geo.analysis.GeoComputation;
import org.meteoinfo.geometry.shape.PolygonShape;
import org.meteoinfo.geometry.shape.ShapeTypes;
import org.meteoinfo.geo.analysis.InterpolationSetting;
import org.meteoinfo.math.interpolate.InterpUtil;
import org.meteoinfo.math.interpolate.KrigingInterpolation2D;

import java.util.ArrayList;
import java.util.List;

public class GeoMathUtil {
    /**
     * 用多边形形状遮罩网格数据
     *
     * @param gridData 网格数据
     * @param aPGS 多边形形状
     * @return 遮罩后的网格数据
     */
    public static GridData maskout(GridData gridData, PolygonShape aPGS) {
        int xNum = gridData.getXNum();
        int yNum = gridData.getYNum();

        GridData cGrid = new GridData(gridData);
        double[] xArray = gridData.getXArray();
        double[] yArray = gridData.getYArray();
        for (int i = 0; i < yNum; i++) {
            if (yArray[i] >= aPGS.getExtent().minY && yArray[i] <= aPGS.getExtent().maxY) {
                for (int j = 0; j < xNum; j++) {
                    if (xArray[j] >= aPGS.getExtent().minX && xArray[j] <= aPGS.getExtent().maxX) {
                        if (GeoComputation.pointInPolygon(aPGS, new PointD(xArray[j], yArray[i]))) {
                            cGrid.setValue(i, j, gridData.getDoubleValue(i, j));
                        } else {
                            cGrid.setValue(i, j, gridData.getDoubleMissingValue());
                        }
                    } else {
                        cGrid.setValue(i, j, gridData.getDoubleMissingValue());
                    }
                }
            } else {
                for (int j = 0; j < xNum; j++) {
                    cGrid.setValue(i, j, gridData.getDoubleMissingValue());
                }
            }
        }

        return cGrid;
    }

    /**
     * 用多边形形状遮罩网格数据
     *
     * @param gridData 网格数据
     * @param polygons 多边形形状
     * @return 遮罩后的网格数据
     */
    public static GridData maskout(GridData gridData, List<PolygonShape> polygons) {
        int xNum = gridData.getXNum();
        int yNum = gridData.getYNum();

        GridData cGrid = new GridData(gridData);
        double[] xArray = gridData.getXArray();
        double[] yArray = gridData.getYArray();
        for (int i = 0; i < yNum; i++) {
            for (int j = 0; j < xNum; j++) {
                if (GeoComputation.pointInPolygons(polygons, new PointD(xArray[j], yArray[i]))) {
                    cGrid.setValue(i, j, gridData.getDoubleValue(i, j));
                } else {
                    cGrid.setValue(i, j, gridData.getDoubleMissingValue());
                }
            }
        }

        return cGrid;
    }


    /**
     * 用多边形图层遮罩网格数据
     *
     * @param gridData 网格数据
     * @param maskLayer 多边形图层
     * @return 遮罩后的网格数据
     */
    public static GridData maskout(GridData gridData, VectorLayer maskLayer) {
        if (maskLayer.getShapeType() != ShapeTypes.POLYGON) {
            return gridData;
        }

        int xNum = gridData.getXNum();
        int yNum = gridData.getYNum();
        GridData cGrid = new GridData(gridData);
        double[] xArray = gridData.getXArray();
        double[] yArray = gridData.getYArray();
        for (int i = 0; i < yNum; i++) {
            if (yArray[i] >= maskLayer.getExtent().minY && yArray[i] <= maskLayer.getExtent().maxY) {
                for (int j = 0; j < xNum; j++) {
                    if (xArray[j] >= maskLayer.getExtent().minX && xArray[j] <= maskLayer.getExtent().maxX) {
                        if (GeoComputation.pointInPolygonLayer(maskLayer, new PointD(xArray[j], yArray[i]), false)) {
                            cGrid.setValue(i, j, gridData.getDoubleValue(i, j));
                        } else {
                            cGrid.setValue(i, j, gridData.getDoubleMissingValue());
                        }
                    } else {
                        cGrid.setValue(i, j, gridData.getDoubleMissingValue());
                    }
                }
            } else {
                for (int j = 0; j < xNum; j++) {
                    cGrid.setValue(i, j, gridData.getDoubleMissingValue());
                }
            }
        }

        return cGrid;
    }

    /**
     * 遮罩站点数据
     *
     * @param stationData 站点数据
     * @param polygonShape 遮罩多边形形状
     * @return 结果站点数据
     */
     public static StationData maskout(StationData stationData, PolygonShape polygonShape) {
         StationData stData = new StationData();
         stData.projInfo = stationData.projInfo;
         stData.missingValue = stationData.missingValue;
         for (int i = 0; i < stationData.getStNum(); i++) {
             if (GeoComputation.pointInPolygon(polygonShape, new PointD(stationData.getX(i), stationData.getY(i)))) {
                 stData.addData(stationData.getStid(i), stationData.getX(i), stationData.getY(i), stationData.getValue(i));
             }
         }

         return stData;
     }

    /**
     * 遮罩站点数据
     *
     * @param stationData 站点数据
     * @param polygonShapes 遮罩多边形形状
     * @return 结果站点数据
     */
     public static StationData maskout(StationData stationData, List<PolygonShape> polygonShapes) {
         StationData stData = new StationData();
         stData.projInfo = stationData.projInfo;
         stData.missingValue = stationData.missingValue;
         for (int i = 0; i < stationData.getStNum(); i++) {
             if (GeoComputation.pointInPolygons(polygonShapes, new PointD(stationData.getX(i), stationData.getY(i)))) {
                 stData.addData(stationData.getStid(i), stationData.getX(i), stationData.getY(i), stationData.getValue(i));
             }
         }

         return stData;
     }

    /**
     * 遮罩站点数据
     *
     * @param stationData 站点数据
     * @param maskLayer 遮罩层
     * @return 结果站点数据
     */
     public static StationData maskout(StationData stationData, VectorLayer maskLayer) {
         if (maskLayer.getShapeType() != ShapeTypes.POLYGON) {
             return stationData;
         }

         List<PolygonShape> polygons = (List<PolygonShape>) maskLayer.getShapes();
         return maskout(stationData, polygons);
     }

    /**
     * 包含站点数据
     *
     * @param stationData 站点数据
     * @param polygonShape 遮罩多边形形状
     * @return 结果站点数据
     */

     public static StationData maskin(StationData stationData, PolygonShape polygonShape) {
         StationData stData = new StationData();
         stData.projInfo = stationData.projInfo;
         stData.missingValue = stationData.missingValue;
         for (int i = 0; i < stationData.getStNum(); i++) {
             if (!GeoComputation.pointInPolygon(polygonShape, new PointD(stationData.getX(i), stationData.getY(i)))) {
                 stData.addData(stationData.getStid(i), stationData.getX(i), stationData.getY(i), stationData.getValue(i));
             }
         }

         return stData;
     }

    /**
     * 包含站点数据
     *
     * @param stationData 站点数据
     * @param polygonShapes 遮罩多边形形状
     * @return 结果站点数据
     */
     public static StationData maskin(StationData stationData, List<PolygonShape> polygonShapes) {
         StationData stData = new StationData();
         stData.projInfo = stationData.projInfo;
         stData.missingValue = stationData.missingValue;
         for (int i = 0; i < stationData.getStNum(); i++) {
             if (!GeoComputation.pointInPolygons(polygonShapes, new PointD(stationData.getX(i), stationData.getY(i)))) {
                 stData.addData(stationData.getStid(i), stationData.getX(i), stationData.getY(i), stationData.getValue(i));
             }
         }

         return stData;
     }

    /**
     * 包含站点数据
     *
     * @param stationData 站点数据
     * @param maskLayer 遮罩层
     * @return 结果站点数据
     */
     public static StationData maskin(StationData stationData, VectorLayer maskLayer) {
         if (maskLayer.getShapeType() != ShapeTypes.POLYGON) {
             return stationData;
         }

         List<PolygonShape> polygons = (List<PolygonShape>) maskLayer.getShapes();
         return maskin(stationData, polygons);
     }

    /**
     * 插值到网格数据
     *
     * @param stationData 站点数据
     * @param interSet 插值设置
     * @return 网格数据
     */

     public static GridData interpolateData(StationData stationData, InterpolationSetting interSet) {
         GridData aGridData = null;
         double[] X;
         double[] Y;
         List<double[]> values = createGridXY(interSet.getGridDataSetting());
         X = values.get(0);
         Y = values.get(1);
         switch (interSet.getInterpolationMethod()) {
             case IDW_RADIUS:
                 stationData.filterData_Radius(interSet.getRadius(), interSet.getGridDataSetting().dataExtent);
                 aGridData = interpolate_Radius(stationData.data,
                         X, Y, interSet.getMinPointNum(), interSet.getRadius(), stationData.missingValue);
                 break;
             case IDW_NEIGHBORS:
                 stationData.filterData_Radius(interSet.getRadius(), interSet.getGridDataSetting().dataExtent);
                 aGridData = interpolate_Neighbor(stationData.data, X, Y,
                         interSet.getMinPointNum(), stationData.missingValue);
                 break;
             case CRESSMAN:
                 stationData.filterData_Radius(0, interSet.getGridDataSetting().dataExtent);
                 aGridData = interpolate_Cressman(stationData.data, X, Y, interSet.getRadiusList(), stationData.missingValue);
                 break;
             case BARNES:
                 stationData.filterData_Radius(0, interSet.getGridDataSetting().dataExtent);
                 double[][] gData = InterpUtil.barnes(stationData.getX(), stationData.getY(), stationData.getStData(),
                         X, Y, interSet.getRadiusList(), 1., 1.);
                 aGridData = new GridData(gData, X, Y);
                 break;
             case KRIGING:
                 stationData.filterData_Radius(0, interSet.getGridDataSetting().dataExtent);
                 gData = InterpUtil.kriging(stationData.getX(), stationData.getY(), stationData.getStData(),
                         X, Y, interSet.getBeta());
                 aGridData = new GridData(gData, X, Y);
                 break;
             case ASSIGN_POINT_GRID:
                 stationData.filterData_Radius(0, interSet.getGridDataSetting().dataExtent);
                 aGridData = interpolate_Assign(stationData.data, X, Y, stationData.missingValue);
                 break;
         }

         return aGridData;
     }

    /**
     * 通过 IDW 半径方法插值
     *
     * @param S 站点数据数组
     * @param X X 坐标数组
     * @param Y Y 坐标数组
     * @param minPNum 最小点数
     * @param radius 半径
     * @param missingValue 缺失值
     * @return 网格数据
     */
     public static GridData interpolate_Radius(double[][] S, double[] X, double[] Y,
             int minPNum, double radius, double missingValue) {
         double[][] dataArray;
         dataArray = wcontour.Interpolate.interpolation_IDW_Radius(S, X, Y, minPNum, radius, missingValue);

         return new GridData(dataArray, X, Y, missingValue);
     }

    /**
     * 通过 IDW 半径方法插值
     *
     * @param stationData 站点数据
     * @param X X 坐标数组
     * @param Y Y 坐标数组
     * @param minPNum 最小点数
     * @param radius 半径
     * @param missingValue 缺失值
     * @return 网格数据
     */

     public static GridData interpolate_Radius(StationData stationData, List<Number> X, List<Number> Y,
             int minPNum, double radius, double missingValue) {
         double[] nX = new double[X.size()];
         double[] nY = new double[Y.size()];
         for (int i = 0; i < X.size(); i++){
             nX[i] = X.get(i).doubleValue();
         }
         for (int i = 0; i < Y.size(); i++){
             nY[i] = Y.get(i).doubleValue();
         }

         return interpolate_Radius(stationData.data, nX, nY, minPNum, radius, missingValue);
     }

    /**
     * 通过 IDW 邻居方法插值
     *
     * @param S 站点数据数组
     * @param X X 坐标数组
     * @param Y Y 坐标数组
     * @param pNum 点数
     * @param missingValue 缺失值
     * @return 网格数据
     */
     public static GridData interpolate_Neighbor(double[][] S, double[] X, double[] Y, int pNum, double missingValue) {
         double[][] dataArray = wcontour.Interpolate.interpolation_IDW_Neighbor(S, X, Y, pNum, missingValue);

         return new GridData(dataArray, X, Y, missingValue);
     }

    /**
     * 通过 IDW 邻居方法插值
     *
     * @param stationData 站点数据
     * @param X X 坐标数组
     * @param Y Y 坐标数组
     * @param pNum 点数
     * @param missingValue 缺失值
     * @return 网格数据
     */
     public static GridData interpolate_Neighbor(StationData stationData, List<Number> X, List<Number> Y, int pNum, double missingValue) {
         double[] nX = new double[X.size()];
         double[] nY = new double[Y.size()];
         for (int i = 0; i < X.size(); i++){
             nX[i] = X.get(i).doubleValue();
         }
         for (int i = 0; i < Y.size(); i++){
             nY[i] = Y.get(i).doubleValue();
         }

         return interpolate_Neighbor(stationData.data, nX, nY, pNum, missingValue);
     }

    /**
     * 通过 Cressman 方法插值
     *
     * @param S 站点数据数组
     * @param X X 坐标数组
     * @param Y Y 坐标数组
     * @param radList 半径列表
     * @param missingValue 缺失值
     * @return 网格数据
     */
     public static GridData interpolate_Cressman(double[][] S, double[] X, double[] Y,
             List<Double> radList, double missingValue) {
         double[][] dataArray = wcontour.Interpolate.cressman(S, X, Y, missingValue, radList);

         return new GridData(dataArray, X, Y, missingValue);
     }

    /**
     * 通过 Cressman 方法插值
     *
     * @param stationData 站点数据
     * @param X X 坐标数组
     * @param Y Y 坐标数组
     * @param radList 半径列表
     * @param missingValue 缺失值
     * @return 网格数据
     */

     public static GridData interpolate_Cressman(StationData stationData, List<Number> X, List<Number> Y,
             List<Number> radList, double missingValue) {
         double[] nX = new double[X.size()];
         double[] nY = new double[Y.size()];
         for (int i = 0; i < X.size(); i++){
             nX[i] = X.get(i).doubleValue();
         }
         for (int i = 0; i < Y.size(); i++){
             nY[i] = Y.get(i).doubleValue();
         }

         List<Double> rlist = new ArrayList<>();
         for (Number r : radList){
             rlist.add(r.doubleValue());
         }
         return interpolate_Cressman(stationData.data, nX, nY, rlist, missingValue);
     }

    /**
     * 通过分配方法插值
     *
     * @param S 站点数据数组
     * @param X X 坐标数组
     * @param Y Y 坐标数组
     * @param missingValue 缺失值
     * @return 网格数据
     */
     public static GridData interpolate_Assign(double[][] S, double[] X, double[] Y, double missingValue) {
         double[][] dataArray = wcontour.Interpolate.assignPointToGrid(S, X, Y, missingValue);

         return new GridData(dataArray, X, Y, missingValue);
     }

    /**
     * 通过分配方法插值
     *
     * @param stationData 站点数据
     * @param X X 坐标数组
     * @param Y Y 坐标数组
     * @param missingValue 缺失值
     * @return 网格数据
     */
     public static GridData interpolate_Assign(StationData stationData, List<Number> X, List<Number> Y, double missingValue) {
         double[] nX = new double[X.size()];
         double[] nY = new double[Y.size()];
         for (int i = 0; i < X.size(); i++){
             nX[i] = X.get(i).doubleValue();
         }
         for (int i = 0; i < Y.size(); i++){
             nY[i] = Y.get(i).doubleValue();
         }

         return interpolate_Assign(stationData.data, nX, nY, missingValue);
     }

     /**
      * 创建栅格 X/Y 坐标
      *
      * @param gSet
      * @return X/Y 坐标数组列表
      */
     public static List<double[]> createGridXY(GridDataSetting gSet) {
         double xDelt = (gSet.dataExtent.maxX - gSet.dataExtent.minX) / (double) (gSet.xNum - 1);
         double yDelt = (gSet.dataExtent.maxY - gSet.dataExtent.minY) / (double) (gSet.yNum - 1);

         return wcontour.Interpolate.createGridXY_Delt(gSet.dataExtent.minX, gSet.dataExtent.minY,
                 gSet.dataExtent.maxX, gSet.dataExtent.maxY, xDelt, yDelt);
     }

}
