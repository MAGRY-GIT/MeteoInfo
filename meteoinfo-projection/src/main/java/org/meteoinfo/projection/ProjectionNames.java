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

/**
 * 定义了一系列地图投影类型。
 * 每个投影类型都有一个唯一的字符串标识符，该标识符对应于Proj4库中的名称。
 * 这些投影类型用于在地图制图中将地球的三维表面映射到二维平面上。
 * 不同的投影类型适用于不同的地理区域和特定的制图需求。
 *
 * @author Yaqiang Wang
 */
public enum ProjectionNames {
    /**
     * 经纬度投影（地理坐标系）
     */
    LongLat("longlat"),

    /**
     * 兰伯特等角圆锥投影
     */
    Lambert_Conformal_Conic("lcc"),

    /**
     * 兰伯特等面积圆锥投影
     */
    Lambert_Equal_Area_Conic("leac"),

    /**
     * 兰伯特等面积方位投影
     */
    Lambert_Azimuthal_Equal_Area("laea"),

    /**
     * 阿尔伯斯等面积投影
     */
    Albers_Equal_Area("aea"),

    /**
     * 艾里投影
     */
    Airy("airy"),

    /**
     * 艾托夫投影
     */
    Aitoff("aitoff"),

    /**
     * 奥古斯特投影
     */
    August("august"),

    /**
     * 方位等距投影
     */
    Azimuthal_Equidistant("aeqd"),

    /**
     * 等距圆锥投影
     */
    Equidistant_Conic("eqdc"),

    /**
     * 立体方位投影（通用）
     */
    Stereographic_Azimuthal("stere"),

    /**
     * 北极立体方位投影
     */
    North_Polar_Stereographic_Azimuthal("stere"),

    /**
     * 南极立体方位投影
     */
    South_Polar_Stereographic_Azimuthal("stere"),

    /**
     * 墨卡托投影
     */
    Mercator("merc"),

    /**
     * 罗宾森投影
     */
    Robinson("robin"),

    /**
     * 摩尔魏德投影
     */
    Molleweide("moll"),

    /**
     * 正射方位投影
     */
    Orthographic_Azimuthal("ortho"),

    /**
     * 地球同步卫星投影
     */
    Geostationary_Satellite("geos"),

    /**
     * 斜轴立体投影（替代）
     */
    Oblique_Stereographic_Alternative("sterea"),

    /**
     * 横轴墨卡托投影
     */
    Transverse_Mercator("tmerc"),

    /**
     * UTM投影（通用横轴墨卡托）
     */
    UTM("utm"),

    /**
     * 辛努索伊德投影
     */
    Sinusoidal("sinu"),

    /**
     * 圆柱等面积投影
     */
    Cylindrical_Equal_Area("cea"),

    /**
     * 哈默-埃克特投影
     */
    Hammer_Eckert("hammer"),

    /**
     * 瓦格纳III投影
     */
    Wagner3("wag3"),

    /**
     * 未定义投影
     */
    Undefine(null);

    private final String proj4Name;

    /**
     * 构造函数，初始化投影类型及其对应的Proj4名称。
     *
     * @param proj4Name Proj4名称
     */
    private ProjectionNames(String proj4Name) {
        this.proj4Name = proj4Name;
    }

    /**
     * 获取投影类型的Proj4名称。
     *
     * @return Proj4名称
     */
    public String getProj4Name() {
        return this.proj4Name;
    }

    /**
     * 根据Proj4名称获取对应的投影类型。
     *
     * @param proj4Name Proj4名称
     * @return 对应的投影类型，如果找不到则返回null
     */
    public static ProjectionNames getName(String proj4Name) {
        for (ProjectionNames name : ProjectionNames.values()) {
            if (name.proj4Name.equals(proj4Name)) {
                return name;
            }
        }
        return null;
    }
}
