 /* Copyright 2012 Yaqiang Wang,
  * yaqiang.wang@gmail.com
  *
  * This library is free software; you can redistribute it and/or modify it
  * under the terms of the GNU Lesser General Public License as published by
  * the Free Software Foundation; either version 2.1 of the License, or (at
  * your option) any later version.
  *
  * This library is distributed in the hope that it will be useful, but
  * WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser
  * General Public License for more details.
  */
 package org.meteoinfo.projection;

 import org.locationtech.proj4j.CRSFactory;
 import org.locationtech.proj4j.CoordinateReferenceSystem;
 import org.locationtech.proj4j.InvalidValueException;
 import org.locationtech.proj4j.datum.Datum;
 import org.locationtech.proj4j.datum.Ellipsoid;
 import org.locationtech.proj4j.parser.Proj4Keyword;
 import org.locationtech.proj4j.parser.Proj4Parser;
 import org.locationtech.proj4j.proj.Projection;
 import org.meteoinfo.common.GridLabel;
 import org.meteoinfo.common.PointD;
 import org.meteoinfo.common.XAlign;
 import org.meteoinfo.common.YAlign;
 import org.meteoinfo.geometry.shape.PolygonShape;
 import org.meteoinfo.ndarray.Array;
 import org.meteoinfo.ndarray.math.ArrayUtil;
 import org.meteoinfo.projection.info.*;

 import java.util.*;

 /**
  * 投影信息抽象类，用于表示地理坐标系和投影坐标系的相关信息。
  *
  * @author Yaqiang Wang
  */
 public abstract class ProjectionInfo {
     // <editor-fold desc="变量">
     /**
      * 预定义的经纬度投影信息实例。
      */
     public final static ProjectionInfo LONG_LAT = factory(new CRSFactory().createFromParameters("WGS84",
             "+title=long/lat:WGS84 +proj=longlat +ellps=WGS84 +datum=WGS84 +units=degrees"));

     /**
      * 坐标参考系统。
      */
     protected CoordinateReferenceSystem crs;

     /**
      * 地图边界。
      */
     protected PolygonShape boundary;

     /**
      * 纬度截断值，适用于某些特定投影。
      */
     protected float cutoff = Float.NaN;
     // </editor-fold>
     // <editor-fold desc="构造方法">

     /**
      * 默认构造函数。
      */
     public ProjectionInfo() {

     }

     /**
      * 构造函数。
      *
      * @param crs 坐标参考系统
      */
     public ProjectionInfo(CoordinateReferenceSystem crs) {
         this.crs = crs;
         updateBoundary();
     }

     /**
      * 构造函数。
      *
      * @param crs    坐标参考系统
      * @param cutoff 纬度截断值
      */
     public ProjectionInfo(CoordinateReferenceSystem crs, float cutoff) {
         this.crs = crs;
         this.cutoff = cutoff;
         updateBoundary();
     }
     // </editor-fold>
     // <editor-fold desc="工厂方法">

     /**
      * 根据坐标参考系统创建新的投影信息实例。
      *
      * @param crs 坐标参考系统
      * @return 投影信息实例
      */
     public static ProjectionInfo factory(CoordinateReferenceSystem crs) {
         ProjectionInfo projInfo;
         Projection proj = crs.getProjection();
         switch (proj.toString()) {
             case "LongLat":
                 projInfo = new LongLat(crs);
                 break;
             case "Albers Equal Area":
                 projInfo = new Albers(crs);
                 break;
             case "Airy":
                 projInfo = new Airy(crs);
                 break;
             case "Aitoff":
             case "Winkel Tripel":
                 projInfo = new Aitoff(crs);
                 break;
             case "August Epicycloidal":
                 projInfo = new August(crs);
                 break;
             case "Equidistant Azimuthal":
                 projInfo = new AzimuthEquidistant(crs);
                 break;
             case "Equidistant Conic":
                 projInfo = new EquidistantConic(crs);
                 break;
             case "Lambert Conformal Conic":
                 projInfo = new LambertConformalConic(crs);
                 break;
             case "Lambert Equal Area Conic":
                 projInfo = new LambertEqualAreaConic(crs);
                 break;
             case "Lambert Azimuthal Equal Area":
                 projInfo = new LambertAzimuthalEqualArea(crs);
                 break;
             case "Stereographic Azimuthal":
                 projInfo = new StereographicAzimuthal(crs);
                 break;
             case "Mercator":
                 projInfo = new Mercator(crs);
                 break;
             case "Robinson":
                 projInfo = new Robinson(crs);
                 break;
             case "Molleweide":
                 projInfo = new Mollweide(crs);
                 break;
             case "Geostationary Satellite":
                 projInfo = new GeostationarySatellite(crs);
                 break;
             case "Sinusoidal":
                 projInfo = new Sinusoidal(crs);
                 break;
             case "Orthographic Azimuthal":
                 projInfo = new OrthographicAzimuthal(crs);
                 break;
             case "Hammer Eckert":
                 projInfo = new Hammer(crs);
                 break;
             case "Universal Tranverse Mercator":
             case "Transverse Mercator":
                 projInfo = new TransverseMercator(crs);
                 break;
             case "Extended Transverse Mercator":
                 projInfo = new UTM(crs);
                 break;
             case "Wagner III":
                 projInfo = new Wagner3(crs);
                 break;
             default:
                 projInfo = new Common(crs);
                 break;
         }

         return projInfo;
     }

     /**
      * 根据Proj4字符串创建新的投影信息实例。
      *
      * @param proj4Str Proj4字符串
      * @return 投影信息实例
      */
     public static ProjectionInfo factory(String proj4Str) {
         CRSFactory crsFactory = new CRSFactory();
         proj4Str = proj4Str.replace("+", " +");
         proj4Str = proj4Str.trim();
         return factory(crsFactory.createFromParameters("custom", proj4Str));
     }

     /**
      * 根据ESRI投影字符串创建新的投影信息实例。
      *
      * @param esriStr ESRI投影字符串
      * @return 投影信息实例
      */
     public static ProjectionInfo factoryESRI(String esriStr) {
         CRSFactory crsFactory = new CRSFactory();
         ProjRegistry registry = new ProjRegistry();
         String[] params = getParameterArray(esriStringToProj4Params(registry, esriStr));
         Proj4Parser parser = new Proj4Parser(crsFactory.getRegistry());
         CoordinateReferenceSystem crs = parser.parse("custom", params);
         return factory(crs);
     }

     /**
      * 根据投影名称创建新的投影信息实例。
      *
      * @param name 投影名称
      * @return 投影信息实例
      */
     public static ProjectionInfo factory(ProjectionNames name) {
         CRSFactory crsFactory = new CRSFactory();
         String proj4Str = "+proj=" + name.getProj4Name();
         return factory(crsFactory.createFromParameters("custom", proj4Str));
     }
     // </editor-fold>
     // <editor-fold desc="Getter和Setter方法">

     /**
      * 获取坐标参考系统。
      *
      * @return 坐标参考系统
      */
     public CoordinateReferenceSystem getCoordinateReferenceSystem() {
         return crs;
     }

     /**
      * 获取投影名称。
      *
      * @return 投影名称
      */
     public abstract ProjectionNames getProjectionName();

     /**
      * 判断是否为经纬度投影。
      *
      * @return 如果是经纬度投影返回true，否则返回false
      */
     public boolean isLonLat() {
         return this.getProjectionName() == ProjectionNames.LongLat;
     }

     /**
      * 获取中心经度。
      *
      * @return 中心经度
      */
     public double getCenterLon() {
         return this.crs.getProjection().getProjectionLongitudeDegrees();
     }

     /**
      * 获取中心纬度。
      *
      * @return 中心纬度
      */
     public double getCenterLat() {
         return this.crs.getProjection().getProjectionLatitudeDegrees();
     }

     /**
      * 获取地图边界。
      *
      * @return 地图边界
      */
     public PolygonShape getBoundary() {
         return this.boundary;
     }

     /**
      * 设置地图边界。
      *
      * @param value 地图边界
      */
     public void setBoundary(PolygonShape value) {
         this.boundary = value;
     }

     /**
      * 获取纬度截断值。
      *
      * @return 纬度截断值
      */
     public float getCutoff() {
         return this.cutoff;
     }

     /**
      * 设置纬度截断值（无实际操作）。
      *
      * @param value 纬度截断值
      */
     public void setCutoff(float value) { }

     /**
      * 设置纬度截断值并更新边界。
      *
      * @param value 纬度截断值
      */
     public void setCutoff_bak(float value) {
         this.cutoff = value;
         this.updateBoundary();
     }
     // </editor-fold>
     // <editor-fold desc="方法">

     /**
      * 获取有效的参数列表。
      *
      * @return 有效参数列表
      */
     public List<String> getValidParas() {
         return new ArrayList<>();
     }

     /**
      * 检查网格标签的位置和对齐方式。
      *
      * @param gl    网格标签
      * @param shift 偏移量
      * @return 包含x/y偏移量和对齐方式的数组
      */
     public Object[] checkGridLabel(GridLabel gl, float shift) {
         float angle = gl.getAngle();
         float xShift = 0.f;
         float yShift = 0.f;
         XAlign xAlign = XAlign.CENTER;
         YAlign yAlign = YAlign.CENTER;

         if (angle == 0) {
             yShift = -shift;
             yAlign = YAlign.BOTTOM;
         } else if (angle == 180) {
             yShift = shift;
             yAlign = YAlign.TOP;
         } else if (angle == 90) {
             xShift = shift;
             xAlign = XAlign.LEFT;
         } else if (angle == 270) {
             xShift = -shift;
             xAlign = XAlign.RIGHT;
         } else if (angle > 0 && angle <= 45) {
             yShift = -shift;
             xAlign = XAlign.LEFT;
             yAlign = YAlign.BOTTOM;
         } else if (angle > 45 && angle < 90) {
             yShift = shift;
             xAlign = XAlign.LEFT;
             yAlign = YAlign.BOTTOM;
         } else if (angle > 90 && angle <= 135) {
             xShift = shift;
             xAlign = XAlign.LEFT;
             yAlign = YAlign.TOP;
         } else if (angle > 135 && angle < 180) {
             yShift = shift;
             xAlign = XAlign.LEFT;
             yAlign = YAlign.TOP;
         } else if (angle > 180 && angle <= 225) {
             yShift = shift;
             xAlign = XAlign.RIGHT;
             yAlign = YAlign.TOP;
         } else if (angle > 225 && angle < 270) {
             xShift = -shift;
             xAlign = XAlign.RIGHT;
             yAlign = YAlign.TOP;
         } else if (angle > 270 && angle <= 315) {
             xShift = -shift;
             xAlign = XAlign.RIGHT;
             yAlign = YAlign.BOTTOM;
         } else if (angle > 315 && angle < 360) {
             yShift = -shift;
             xAlign = XAlign.RIGHT;
             yAlign = YAlign.BOTTOM;
         }

         return new Object[]{xShift, yShift, xAlign, yAlign};
     }

     /**
      * 更新地图边界（空实现）。
      */
     public void updateBoundary() {}

     /**
      * 使用椭圆定义投影边界。
      *
      * @param semimajor 半长轴
      * @param semiminor 半短轴
      * @param easting   东向偏移
      * @param northing  北向偏移
      * @param n         点数
      * @return 椭圆边界点列表
      */
     protected List<PointD> ellipse_boundary(double semimajor, double semiminor, double easting, double northing, int n) {
         Array t = ArrayUtil.lineSpace(0, -2 * Math.PI, n, true);
         List<PointD> r = new ArrayList<>();
         double x, y;
         for (int i = 0; i < t.getSize(); i++) {
             x = semimajor * Math.cos(t.getDouble(i)) + easting;
             y = semiminor * Math.sin(t.getDouble(i)) + northing;
             r.add(new PointD(x, y));
         }

         return r;
     }

     /**
      * 获取投影操作的参考切割经度。
      *
      * @return 参考切割经度
      */
     public double getRefCutLon() {
         double refLon = this.getCoordinateReferenceSystem().getProjection().getProjectionLongitudeDegrees();
         refLon += 180;
         if (refLon > 180) {
             refLon = refLon - 360;
         } else if (refLon < -180) {
             refLon = refLon + 360;
         }
         return refLon;
     }

     /**
      * 获取Proj4字符串。
      *
      * @return Proj4字符串
      */
     public String toProj4String() {
         return crs.getParameterString();
     }

     /**
      * 计算椭球的反扁率。
      *
      * @param ellipsoid 椭球体
      * @return 反扁率
      */
     public double getInverseFlattening(Ellipsoid ellipsoid) {
         if (ellipsoid.poleRadius == ellipsoid.equatorRadius) {
             return 0; // 防止球体时除以零
         }
         return (ellipsoid.equatorRadius) / (ellipsoid.equatorRadius - ellipsoid.poleRadius);
     }

     /**
      * 将参数映射转换为字符串数组。
      *
      * @param params 参数映射
      * @return 字符串数组
      */
     private static String[] getParameterArray(Map params) {
         String[] args = new String[params.size()];
         int i = 0;
         Set<String> key = params.keySet();
         for (String s : key) {
             args[i] = "+" + s + "=" + params.get(s);
             i += 1;
         }

         return args;
     }

     /**
      * 将ESRI投影字符串转换为Proj4参数映射。
      *
      * @param registry 注册表
      * @param esriString ESRI投影字符串
      * @return Proj4参数映射
      */
     public static Map esriStringToProj4Params(ProjRegistry registry, String esriString) {
         Map params = new HashMap();
         String key, value, name;
         int iStart, iEnd;

         // 投影
         if (!esriString.contains("PROJCS")) {
             key = Proj4Keyword.proj;
             value = "longlat";
             params.put(key, value);
         } else {
             Projection projection = null;
             iStart = esriString.indexOf("PROJECTION") + 12;
             iEnd = esriString.indexOf("]", iStart) - 1;
             String s = esriString.substring(iStart, iEnd);
             if (s != null) {
                 projection = registry.getProjectionEsri(s);
                 if (projection == null) {
                     throw new InvalidValueException("未知的投影: " + s);
                 }
             }

             String proj4Name = registry.getProj4Name(projection);
             key = Proj4Keyword.proj;
             value = proj4Name;
             params.put(key, value);
         }

         // 大地基准面
         if (esriString.contains("DATUM")) {
             iStart = esriString.indexOf("DATUM") + 7;
             iEnd = esriString.indexOf(",", iStart) - 1;
             if (iEnd > iStart) {
                 key = Proj4Keyword.datum;
                 value = esriString.substring(iStart, iEnd);
                 if (value.equals("D_WGS_1984")) {
                     value = "WGS84";
                 } else {
                     value = "WGS84";
                 }
                 params.put(key, value);
             }
         }

         // 椭球体
         if (esriString.contains("SPHEROID")) {
             iStart = esriString.indexOf("SPHEROID") + 9;
             iEnd = esriString.indexOf("]", iStart);
             if (iEnd > iStart) {
                 String extracted = esriString.substring(iStart, iEnd);
                 String[] terms = extracted.split(",");
                 name = terms[0];
                 name = name.substring(1, name.length() - 1);
                 if (name.equals("WGS_1984")) {
                     name = "WGS84";
                 } else {
                     name = "WGS84";
                 }
                 key = Proj4Keyword.ellps;
                 value = name;
                 params.put(key, value);
                 key = Proj4Keyword.a;
                 value = terms[1];
                 params.put(key, value);
                 key = Proj4Keyword.rf;
                 value = terms[2];
                 params.put(key, value);
             }
         }

//        //Primem
 //        if (esriString.contains("PRIMEM")) {
 //            iStart = esriString.indexOf("PRIMEM") + 7;
 //            iEnd = esriString.indexOf("]", iStart);
 //            if (iEnd > iStart) {
 //                String extracted = esriString.substring(iStart, iEnd);
 //                String[] terms = extracted.split(",");
 //                name = terms[0];
 //                name = name.substring(1, name.length() - 1);
 //                key = Proj4Keyword.pm;
 //                value = terms[1];
 //                params.put(key, value);
 //            }
 //        }

         //投影参数
         value = getParameter("False_Easting", esriString);
         if (value != null) {
             key = Proj4Keyword.x_0;
             params.put(key, value);
         }
         value = getParameter("False_Northing", esriString);
         if (value != null) {
             key = Proj4Keyword.y_0;
             params.put(key, value);
         }
         value = getParameter("Central_Meridian", esriString);
         if (value != null) {
             key = Proj4Keyword.lon_0;
             params.put(key, value);
         }
         value = getParameter("Standard_Parallel_1", esriString);
         if (value != null) {
             key = Proj4Keyword.lat_1;
             params.put(key, value);
         }
         value = getParameter("Standard_Parallel_2", esriString);
         if (value != null) {
             key = Proj4Keyword.lat_2;
             params.put(key, value);
         }
         value = getParameter("Scale_Factor", esriString);
         if (value != null) {
             key = Proj4Keyword.k_0;
             params.put(key, value);
         }
         value = getParameter("Latitude_Of_Origin", esriString);
         if (value != null) {
             key = Proj4Keyword.lat_0;
             params.put(key, value);
         }

         //Unit

         return params;
     }
    /**
     * 从ESRI字符串中提取指定参数的值。
     *
     * @param name 参数名称
     * @param esriString ESRI格式的字符串
     * @return 提取的参数值，若未找到则返回null
     */
    private static String getParameter(String name, String esriString) {
        String result = null;
        String par = "PARAMETER[\"" + name;
        int iStart = esriString.toLowerCase().indexOf(par.toLowerCase());
        if (iStart >= 0) {
            iStart += 13 + name.length();
            int iEnd = esriString.indexOf(",", iStart) - 1;
            if (iEnd < 0)
                iEnd = esriString.length() - 2;
            result = esriString.substring(iStart, iEnd);
        }
        return result;
    }

    /**
     * 将椭球体信息转换为ESRI格式的字符串。
     *
     * @param ellipsoid 椭球体对象
     * @return ESRI格式的椭球体字符串
     */
    public String toEsriString(Ellipsoid ellipsoid) {
        return "SPHEROID[\"" + ellipsoid.getName() + "\"," + ellipsoid.getEquatorRadius() + "," + getInverseFlattening(ellipsoid) + "]";
    }

    /**
     * 将大地基准面信息转换为ESRI格式的字符串。
     *
     * @param datum 大地基准面对象
     * @return ESRI格式的大地基准面字符串
     */
    public String toEsriString(Datum datum) {
        return "DATUM[\"" + datum.getName() + "\"," + toEsriString(datum.getEllipsoid()) + "]";
    }

    /**
     * 获取ESRI投影字符串。
     *
     * @return ESRI格式的投影字符串
     */
    public String toEsriString() {
        String result = "";
        String geoName = "GCS_WGS_1984";
        Projection proj = this.crs.getProjection();
        if (proj.getName().equals("longlat")) {
            // 如果是经纬度投影，生成地理坐标系字符串
            result = "GEOGCS[\"" + geoName + "\"," + toEsriString(this.crs.getDatum()) + "," + "PRIMEM[\"Greenwich\",0.0]"
                    + "," + "UNIT[\"Degree\",0.0174532925199433]" + "]";
            return result;
        } else {
            // 否则生成投影坐标系字符串
            String name = "Custom";
            result = "PROJCS[\"" + name + "\"," + "GEOGCS[\"" + geoName + "\"," + toEsriString(this.crs.getDatum()) + ","
                    + "PRIMEM[\"Greenwich\",0.0]" + "," + "UNIT[\"Degree\",0.0174532925199433]" + "]" + ", ";
        }

        // 添加投影参数
        result += "PROJECTION[\"" + proj.getName() + "\"],";
        result += "PARAMETER[\"False_Easting\"," + String.valueOf(proj.getFalseEasting()) + "],";
        result += "PARAMETER[\"False_Northing\"," + String.valueOf(proj.getFalseNorthing()) + "],";
        result += "PARAMETER[\"Central_Meridian\"," + String.valueOf(proj.getProjectionLongitudeDegrees()) + "],";
        result += "PARAMETER[\"Standard_Parallel_1\"," + String.valueOf(proj.getProjectionLatitude1Degrees()) + "],";
        result += "PARAMETER[\"Standard_Parallel_2\"," + String.valueOf(proj.getProjectionLatitude2Degrees()) + "],";
        result += "PARAMETER[\"Scale_Factor\"," + String.valueOf(proj.getScaleFactor()) + "],";
        result += "PARAMETER[\"Latitude_Of_Origin\"," + String.valueOf(proj.getProjectionLatitudeDegrees()) + "],";
        result += "UNIT[\"Meter\",1.0]]";
        return result;
    }

    /**
     * 返回Proj4格式的字符串表示。
     *
     * @return Proj4格式的字符串
     */
    @Override
    public String toString() {
        return crs.getParameterString();
    }

    /**
     * 比较两个投影是否相等。
     *
     * @param projA 投影A
     * @param projB 投影B
     * @return 如果两个投影相等返回true，否则返回false
     */
    public boolean equals(Projection projA, Projection projB) {
        if (!projA.getName().equals(projB.getName()))
            return false;
        if (projA.getEquatorRadius() != projB.getEquatorRadius())
            return false;
        if (projA.getEllipsoid().eccentricity != projB.getEllipsoid().eccentricity)
            return false;
        if (!projA.getEllipsoid().isEqual(projA.getEllipsoid(), 0.0000001))
            return false;
        if (projA.getEllipsoid().eccentricity2 != projB.getEllipsoid().eccentricity2)
            return false;
        if (projA.getFalseEasting() != projB.getFalseEasting())
            return false;
        if (projA.getFalseNorthing() != projB.getFalseNorthing())
            return false;
        if (projA.getFromMetres() != projB.getFromMetres())
            return false;
        if (projA.getProjectionLatitudeDegrees() != projB.getProjectionLatitudeDegrees())
            return false;
        if (projA.getProjectionLatitude1Degrees() != projB.getProjectionLatitude1Degrees())
            return false;
        if (projA.getProjectionLongitudeDegrees() != projB.getProjectionLongitudeDegrees())
            return false;
        if (projA.getScaleFactor() != projB.getScaleFactor())
            return false;
        if (projA.getTrueScaleLatitudeDegrees() != projB.getTrueScaleLatitudeDegrees())
            return false;

        return true;
    }

    /**
     * 判断当前投影信息是否与另一个投影信息相同。
     *
     * @param projInfo 另一个投影信息对象
     * @return 如果两个投影信息相同返回true，否则返回false
     */
    public boolean equals(ProjectionInfo projInfo) {
        if (this.getProjectionName() == ProjectionNames.LongLat && projInfo.getProjectionName() == ProjectionNames.LongLat)
            return true;
        else {
            String proj4Str1 = this.toProj4String();
            String proj4Str2 = projInfo.toProj4String();
            if (proj4Str1.equals(proj4Str2))
                return true;
            else {
                if (!this.crs.getDatum().isEqual(projInfo.crs.getDatum()))
                    return false;
                return equals(this.crs.getProjection(), projInfo.crs.getProjection());
            }
        }
    }

    /**
     * 根据标准纬线计算比例因子。
     *
     * @param stP 标准纬线（单位：度）
     * @return 计算得到的比例因子
     */
    public static double calScaleFactorFromStandardParallel(double stP) {
        double e = 0.081819191; // 椭球体的第一偏心率
        stP = Math.PI * stP / 180; // 转换为弧度
        double tF;
        if (stP > 0) {
            tF = Math.tan(Math.PI / 4.0 - stP / 2.0) * (Math.pow((1.0 + e * Math.sin(stP)) / (1.0 - e * Math.sin(stP)), e / 2.0));
        } else {
            tF = Math.tan(Math.PI / 4.0 + stP / 2.0) / (Math.pow((1.0 + e * Math.sin(stP)) / (1.0 - e * Math.sin(stP)), e / 2.0));
        }

        double mF = Math.cos(stP) / Math.pow(1.0 - e * e * Math.pow(Math.sin(stP), 2.0), 0.5);
        double k0 = mF * (Math.pow(Math.pow(1.0 + e, 1.0 + e) * Math.pow(1.0 - e, 1.0 - e), 0.5)) / (2.0 * tF);

        return k0;
    }

    /**
     * 克隆当前投影信息对象。
     *
     * @return 新的ProjectionInfo对象
     */
    public Object clone() {
        return ProjectionInfo.factory(this.toProj4String());
    }

     // </editor-fold>
 }
