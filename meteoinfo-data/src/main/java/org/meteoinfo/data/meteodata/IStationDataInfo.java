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

import org.meteoinfo.data.StationData;

/**
 * 站点数据信息接口。
 * 提供读取站点数据、站点信息数据和站点模型数据的方法。
 *
 * @author Yaqiang Wang
 */
public interface IStationDataInfo {

    // <editor-fold desc="Methods">

    /**
     * 读取站点数据。
     * 根据时间索引、变量名称和层次索引获取站点数据。
     *
     * @param timeIdx 时间索引，表示数据的时间点。
     * @param varName 变量名称，表示要读取的数据变量。
     * @param levelIdx 层次索引，表示数据的层次。
     * @return 返回包含站点数据的 {@link StationData} 对象。
     */
    public abstract StationData getStationData(int timeIdx, String varName, int levelIdx);

    /**
     * 读取站点信息数据。
     * 根据时间索引和层次索引获取站点信息数据。
     *
     * @param timeIdx 时间索引，表示数据的时间点。
     * @param levelIdx 层次索引，表示数据的层次。
     * @return 返回包含站点信息数据的 {@link StationInfoData} 对象。
     */
    public abstract StationInfoData getStationInfoData(int timeIdx, int levelIdx);

    /**
     * 读取站点模型数据。
     * 根据时间索引和层次索引获取站点模型数据。
     *
     * @param timeIdx 时间索引，表示数据的时间点。
     * @param levelIdx 层次索引，表示数据的层次。
     * @return 返回包含站点模型数据的 {@link StationModelData} 对象。
     */
    public abstract StationModelData getStationModelData(int timeIdx, int levelIdx);

    // </editor-fold>
}
