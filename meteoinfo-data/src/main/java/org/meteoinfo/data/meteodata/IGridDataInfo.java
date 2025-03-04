/*
 * Copyright 2012 Yaqiang Wang,
 * yaqiang.wang@gmail.com
 *
 * 这个库是自由软件；你可以根据GNU Lesser通用公共许可证的条款重新分发和/或修改它，
 * 由自由软件基金会发布；要么遵守GNU Lesser通用公共许可证的第2.1版，
 * 或者（由你选择）任何更新版本。
 *
 * 本库在希望它是有用的，但没有任何保证；甚至没有暗示的适销性或适用性的保证。
 * 特定目的。有关详细信息，请参阅GNU Lesser通用公共许可证。
 */
package org.meteoinfo.data.meteodata;

import org.meteoinfo.data.GridArray;
import org.meteoinfo.data.GridData;

/**
 * 网格数据信息接口。
 * 提供了读取不同维度（时间、层次、经度、纬度）网格数据的方法。
 *
 * @author Yaqiang Wang
 */
public interface IGridDataInfo {

    // <editor-fold desc="Methods">

    /**
     * 获取网格数组。
     *
     * @param varName 变量名称
     * @return 网格数组
     */
    public abstract GridArray getGridArray(String varName);

    /**
     * 读取网格数据 - 经度/纬度。
     *
     * @param timeIdx 时间索引
     * @param varName 变量名称
     * @param levelIdx 层次索引
     * @return 网格数据
     */
    public abstract GridData getGridData_LonLat(int timeIdx, String varName, int levelIdx);

    /**
     * 读取网格数据 - 时间/纬度。
     *
     * @param lonIdx 经度索引
     * @param varName 变量名称
     * @param levelIdx 层次索引
     * @return 网格数据
     */
    public abstract GridData getGridData_TimeLat(int lonIdx, String varName, int levelIdx);

    /**
     * 读取网格数据 - 时间/经度。
     *
     * @param latIdx 纬度索引
     * @param varName 变量名称
     * @param levelIdx 层次索引
     * @return 网格数据
     */
    public abstract GridData getGridData_TimeLon(int latIdx, String varName, int levelIdx);

    /**
     * 读取网格数据 - 层次/纬度。
     *
     * @param lonIdx 经度索引
     * @param varName 变量名称
     * @param timeIdx 时间索引
     * @return 网格数据
     */
    public abstract GridData getGridData_LevelLat(int lonIdx, String varName, int timeIdx);

    /**
     * 读取网格数据 - 层次/经度。
     *
     * @param latIdx 纬度索引
     * @param varName 变量名称
     * @param timeIdx 时间索引
     * @return 网格数据
     */
    public abstract GridData getGridData_LevelLon(int latIdx, String varName, int timeIdx);

    /**
     * 读取网格数据 - 层次/时间。
     *
     * @param latIdx 纬度索引
     * @param varName 变量名称
     * @param lonIdx 经度索引
     * @return 网格数据
     */
    public abstract GridData getGridData_LevelTime(int latIdx, String varName, int lonIdx);

    /**
     * 读取网格数据 - 时间。
     *
     * @param lonIdx 经度索引
     * @param latIdx 纬度索引
     * @param varName 变量名称
     * @param levelIdx 层次索引
     * @return 网格数据
     */
    public abstract GridData getGridData_Time(int lonIdx, int latIdx, String varName, int levelIdx);

    /**
     * 读取网格数据 - 层次。
     *
     * @param lonIdx 经度索引
     * @param latIdx 纬度索引
     * @param varName 变量名称
     * @param timeIdx 时间索引
     * @return 网格数据
     */
    public abstract GridData getGridData_Level(int lonIdx, int latIdx, String varName, int timeIdx);

    /**
     * 读取网格数据 - 经度。
     *
     * @param timeIdx 时间索引
     * @param latIdx 纬度索引
     * @param varName 变量名称
     * @param levelIdx 层次索引
     * @return 网格数据
     */
    public abstract GridData getGridData_Lon(int timeIdx, int latIdx, String varName, int levelIdx);

    /**
     * 读取网格数据 - 纬度。
     *
     * @param timeIdx 时间索引
     * @param lonIdx 经度索引
     * @param varName 变量名称
     * @param levelIdx 层次索引
     * @return 网格数据
     */
    public abstract GridData getGridData_Lat(int timeIdx, int lonIdx, String varName, int levelIdx);
    // </editor-fold>
 }
