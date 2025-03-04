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

import org.meteoinfo.projection.ProjectionInfo;
import org.locationtech.proj4j.CRSFactory;

/**
 *
 * @author Yaqiang Wang
 */
public class World extends CoordinateSystemCategory {
    CRSFactory _crsFactory = new CRSFactory();
    public final ProjectionInfo WGS1984;
    /**
     * 构造函数用于初始化World对象
     * 在此构造函数中，我们将初始化WGS1984投影信息
     * 这个投影信息是基于WGS84坐标参考系统创建的，使用的是经纬度坐标系
     */
    public World(){
        // 定义WGS84坐标参考系统的参数字符串
        final String WGS84_PARAM = "+title=long/lat:WGS84 +proj=longlat +ellps=WGS84 +datum=WGS84 +units=degrees";

        // 使用工厂方法根据参数字符串创建WGS84投影信息对象
        WGS1984 = ProjectionInfo.factory(_crsFactory.createFromParameters("WGS84", WGS84_PARAM));
    }
}
