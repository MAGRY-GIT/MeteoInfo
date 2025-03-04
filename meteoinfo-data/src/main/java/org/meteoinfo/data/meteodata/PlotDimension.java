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
package org.meteoinfo.data.meteodata;

/**
 *
 * @author Yaqiang Wang
 */
public enum PlotDimension {
    /// <summary>
        /// 表示经纬度组合的维度类型
        /// </summary>
        Lat_Lon,
        /// <summary>
        /// 表示时间与经度组合的维度类型
        /// </summary>
        Time_Lon,
        /// <summary>
        /// 表示时间与纬度组合的维度类型
        /// </summary>
        Time_Lat,
        /// <summary>
        /// 表示层次与经度组合的维度类型
        /// </summary>
        Level_Lon,
        /// <summary>
        /// 表示层次与纬度组合的维度类型
        /// </summary>
        Level_Lat,
        /// <summary>
        /// 表示层次与时间组合的维度类型
        /// </summary>
        Level_Time,
        /// <summary>
        /// 表示时间维度类型
        /// </summary>
        Time,
        /// <summary>
        /// 表示层次维度类型
        /// </summary>
        Level,
        /// <summary>
        /// 表示经度维度类型
        /// </summary>
        Lon,
        /// <summary>
        /// 表示纬度维度类型
        /// </summary>
        Lat,
}
