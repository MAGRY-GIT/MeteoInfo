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

import java.io.File;

import org.meteoinfo.common.MIMath;
import org.meteoinfo.data.GridData;
import org.meteoinfo.data.StationData;
import org.meteoinfo.data.meteodata.arl.ARLDataInfo;
import org.meteoinfo.data.meteodata.ascii.ASCIIGridDataInfo;
import org.meteoinfo.data.meteodata.ascii.LonLatStationDataInfo;
import org.meteoinfo.data.meteodata.ascii.SurferGridDataInfo;
import org.meteoinfo.data.meteodata.awx.AWXDataInfo;
import org.meteoinfo.data.meteodata.bandraster.BILDataInfo;
import org.meteoinfo.data.meteodata.bandraster.GeoTiffDataInfo;
import org.meteoinfo.data.meteodata.grads.GrADSDataInfo;
import org.meteoinfo.data.meteodata.hysplit.HYSPLITConcDataInfo;
import org.meteoinfo.data.meteodata.hysplit.HYSPLITPartDataInfo;
import org.meteoinfo.data.meteodata.hysplit.HYSPLITTrajDataInfo;
import org.meteoinfo.data.meteodata.matlab.MatLabDataInfo;
import org.meteoinfo.data.meteodata.micaps.*;
import org.meteoinfo.data.meteodata.netcdf.NetCDFDataInfo;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.reflect.InvocationTargetException;
import java.time.LocalDateTime;
import java.time.Duration;
import java.util.ArrayList;

import org.meteoinfo.data.meteodata.radar.RadarDataUtil;
import org.meteoinfo.ndarray.math.ArrayMath;
import org.meteoinfo.projection.ProjectionInfo;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.meteoinfo.data.meteodata.mm5.MM5DataInfo;
import org.meteoinfo.data.meteodata.mm5.MM5IMDataInfo;
import org.meteoinfo.data.mathparser.MathParser;
import org.meteoinfo.data.mathparser.ParseException;
import org.meteoinfo.data.meteodata.metar.METARDataInfo;
import org.meteoinfo.data.meteodata.synop.SYNOPDataInfo;
import org.meteoinfo.ndarray.Array;
import org.meteoinfo.ndarray.InvalidRangeException;
import org.meteoinfo.ndarray.Range;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFiles;

/**
 *
 * @author Yaqiang Wang
 * MeteoDataInfo 类表示气象数据信息。
 * 主要用于封装各种类型的气象数据，便于管理和访问。
 */
public class MeteoDataInfo {
    // <editor-fold desc="Variables">

    private PlotDimension _dimensionSet = PlotDimension.Lat_Lon;
    private String varName;
    private int _timeIdx;
    private int _levelIdx;
    private int _latIdx;
    private int _lonIdx;
    private DrawType2D drawType2D;
    /// <summary>
    /// Is Lon/Lat
    /// </summary>
    public boolean IsLonLat;
    /// <summary>
    /// If the U/V of the wind are along latitude/longitude.
    /// </summary>
    public boolean EarthWind;
    private DataInfo dataInfo;
    /// <summary>
    /// Data information text
    /// </summary>
    private String _infoText;
    /// <summary>
    /// Wind U/V variable name
    /// </summary>
    private MeteoUVSet _meteoUVSet;
    /// <summary>
    /// If X reserved
    /// </summary>
    public boolean xReserve;
    /// <summary>
    /// If Y reserved
    /// </summary>
    public boolean yReserve;
    // </editor-fold>
    // <editor-fold desc="Constructor">

    /**
     * Constructor
     */
    /**
     * 构造函数用于初始化MeteoDataInfo对象
     * 该构造函数设置了对象的初始状态，为对象的属性赋予了默认值
     */
    public MeteoDataInfo() {
        // 初始化dataInfo为null，表示尚未设置任何数据信息
        dataInfo = null;
        // 默认设置IsLonLat为true，表示使用经纬度数据
        IsLonLat = true;
        // 默认设置EarthWind为true，表示使用地球风数据
        EarthWind = true;
        // 初始化_infoText为空字符串，用于存储信息文本
        _infoText = "";
        // 初始化_meteoUVSet为一个新的MeteoUVSet对象，用于存储紫外线信息
        _meteoUVSet = new MeteoUVSet();
        // 默认设置xReserve为false，表示X轴数据不进行反转
        xReserve = false;
        // 默认设置yReserve为false，表示Y轴数据不进行反转
        yReserve = false;
    }
    // </editor-fold>
    // <editor-fold desc="Get Set Methods">

    /**
     * Get data info
     *
     * @return Data info
     */
    public DataInfo getDataInfo() {
        return dataInfo;
    }

    /**
     * Set data info
     *
     * @param value Data info
     */
    public void setDataInfo(DataInfo value) {
        dataInfo = value;
        _infoText = dataInfo.generateInfoText();
    }

    /**
     * Get projection info
     *
     * @return Projection info
     */
    public ProjectionInfo getProjectionInfo() {
        return dataInfo.getProjectionInfo();
    }

    /**
     * Get meteo data type
     *
     * @return Meteo data type
     */
    public MeteoDataType getDataType() {
        return this.dataInfo.getDataType();
    }

    /**
     * Get plot dimension
     *
     * @return Plot dimension
     */
    public PlotDimension getDimensionSet() {
        return _dimensionSet;
    }

    /**
     * Set plot dimension
     *
     * @param value Plot dimension
     */
    public void setDimensionSet(PlotDimension value) {
        _dimensionSet = value;
    }

    /**
     * Get data info text
     *
     * @return Data info text
     */
    public String getInfoText() {
        return _infoText;
    }

    /**
     * Get time index
     *
     * @return Time index
     */
    public int getTimeIndex() {
        return _timeIdx;
    }

    /**
     * Set time index
     *
     * @param value Time index
     */
    public void setTimeIndex(int value) {
        _timeIdx = value;
    }

    /**
     * Get level index
     *
     * @return Level index
     */
    public int getLevelIndex() {
        return _levelIdx;
    }

    /**
     * Set level index
     *
     * @param value Level index
     */
    public void setLevelIndex(int value) {
        _levelIdx = value;
    }

    /**
     * Get variable name
     *
     * @return Variable name
     */
    public String getVariableName() {
        if (this.varName == null && this.getDataInfo() != null) {
            return this.getDataInfo().getVariableNames().get(0);
        } else
            return varName;
    }

    /**
     * 设置变量名称
     *
     * @param value Variable name
     */
    public void setVariableName(String value) {
        varName = value;
    }

    /**
     *获取经度索引
     *
     * @return Longitude index
     */
    public int getLonIndex() {
        return _lonIdx;
    }

    /**
     * Set longitude index
     *
     * @param value Longitude index
     */
    public void setLonIndex(int value) {
        _lonIdx = value;
    }

    /**
     * Get latitude index
     *
     * @return Latitude index
     */
    public int getLatIndex() {
        return _latIdx;
    }

    /**
     * Set latitude index
     *
     * @param value Latitude index
     */
    public void setLatIndex(int value) {
        _latIdx = value;
    }

    /**
     * Get DrawType2D
     * @return DrawType2D
     */
    public DrawType2D getDrawType2D() {
        return this.drawType2D;
    }

    /**
     * Set DrawType2D
     * @param value DrawType2D
     */
    public void setDrawType2D(DrawType2D value) {
        this.drawType2D = value;
    }

    /**
     * Get Meteo U/V setting
     *
     * @return Meteo U/V setting
     */
    public MeteoUVSet getMeteoUVSet() {
        return _meteoUVSet;
    }

    /**
     * Set Meteo U/V Setting
     *
     * @param value Meteo U/V setting
     */
    public void setMeteoUVSet(MeteoUVSet value) {
        _meteoUVSet = value;
    }

    /**
     * Get missing value
     *
     * @return Missing value
     */
    public double getMissingValue() {
        return dataInfo.getMissingValue();
    }

    /**
     * Get if is grid data
     *
     * @return Boolean
     */
    public boolean isGridData() {

        switch (this.getDataType()) {
            case ARL_GRID:
            case ASCII_GRID:
            case GRADS_GRID:
            case GRIB1:
            case GRIB2:
            case HYSPLIT_CONC:
            case MICAPS_11:
            case MICAPS_13:
            case MICAPS_4:
            case MICAPS_131:
            case SURFER_GRID:
            case MM5:
            case MM5IM:
                return true;
            case NETCDF:
                if (((NetCDFDataInfo) dataInfo).isSWATH()) {
                    return false;
                } else {
                    return true;
                }
            case GEOTIFF:
                return true;
            case AWX:
                switch (((AWXDataInfo) this.getDataInfo()).getProductType()) {
                    case 1:
                    case 2:
                    case 3:
                        return true;
                    default:
                        return false;
                }
            case MICAPS_MDFS:
                switch (((MDFSDataInfo) dataInfo).getType()) {
                    case 4:
                    case 11:
                        return true;
                    default:
                        return false;
                }
            default:
                return false;
        }
    }

    /**
     * Get if is station data
     *
     * @return Boolean
     */
    public boolean isStationData() {
        switch (this.getDataType()) {
            case GRADS_STATION:
            case ISH:
            case METAR:
            case MICAPS_1:
            case MICAPS_2:
            case MICAPS_3:
            case MICAPS_120:
            case LON_LAT_STATION:
            case SYNOP:
            case HYSPLIT_PARTICLE:
                return true;
            case AWX:
                if (((AWXDataInfo) this.getDataInfo()).getProductType() == 4) {
                    return true;
                } else {
                    return false;
                }
            case MICAPS_MDFS:
                switch (((MDFSDataInfo) this.dataInfo).getType()) {
                    case 1:
                    case 2:
                        return true;
                }
            default:
                return false;
        }
    }

    /**
     * Get if is trajectory data
     *
     * @return Boolean
     */
    public boolean isTrajData() {
        switch (this.getDataType()) {
            case HYSPLIT_TRAJ:
            case MICAPS_7:
                return true;
            default:
                return false;
        }
    }

    /**
     * Get if is SWATH data
     *
     * @return Boolean
     */
    public boolean isSWATHData() {
        switch (this.getDataType()) {
            case NETCDF:
                if (((NetCDFDataInfo) dataInfo).isSWATH()) {
                    return true;
                }
            default:
                return false;
        }
    }

    /**
     * Get variable dimension number
     *
     * @return Variable dimension number
     */
    public int getDimensionNumber() {
        int dn = 2;
        switch (_dimensionSet) {
            case Lat_Lon:
            case Level_Lat:
            case Level_Lon:
            case Level_Time:
            case Time_Lat:
            case Time_Lon:
                dn = 2;
                break;
            case Level:
            case Lon:
            case Time:
            case Lat:
                dn = 1;
                break;
        }

        return dn;
    }

    // </editor-fold>
    // <editor-fold desc="Methods">
    // <editor-fold desc="Open Data">

    /**
     * Get data info from a file name
     * @param fileName The file name
     * @return Data info
     */
    public DataInfo getDataInfo(String fileName) {
        DataInfo di = RadarDataUtil.getDataInfo(fileName);
        if (di == null) {
            try {
                RandomAccessFile raf = new RandomAccessFile(fileName, "r");
                if (GrADSDataInfo.class.getDeclaredConstructor().newInstance().isValidFile(raf)) {
                    di = new GrADSDataInfo();
                } else if (NetcdfFiles.canOpen(fileName)) {
                    di = new NetCDFDataInfo();
                } else if (ARLDataInfo.class.getDeclaredConstructor().newInstance().isValidFile(raf)) {
                    di = new ARLDataInfo();
                } else if (MatLabDataInfo.class.getDeclaredConstructor().newInstance().isValidFile(raf)) {
                    di = new MatLabDataInfo();
                } else {
                    di = MICAPSDataInfo.getDataInfo(raf);
                }
                raf.close();
            } catch (IOException | NoSuchMethodException ex) {
                return null;
            } catch (InvocationTargetException e) {
                return null;
            } catch (InstantiationException e) {
                return null;
            } catch (IllegalAccessException e) {
                return null;
            }
        }

        return di;
    }

    /**
     * Open data file
     *
     * @param fileName File name
     */
    public void openData(String fileName) {
        openData(fileName, false);
    }

    /**
     * Open data file
     *
     * @param fileName File name
     * @param keepOpen Keep the file opened or not
     */
    public void openData(String fileName, boolean keepOpen) {
        this.dataInfo = getDataInfo(fileName);
        if (dataInfo != null) {
            if (dataInfo.getClass() == NetCDFDataInfo.class) {
                dataInfo.readDataInfo(fileName, keepOpen);
            } else {
                dataInfo.readDataInfo(fileName);
            }
            this._infoText = dataInfo.generateInfoText();
        }
    }
    
    /**
     * Open data file
     *
     * @param ncfile Netcdf file
     * @param keepOpen Keep the file opened or not
     */
    public void openData(NetcdfFile ncfile, boolean keepOpen) {
        this.openNetCDFData(ncfile, keepOpen);
    }

    /**
     * Close opened file
     */
    public void close() {
        if (this.dataInfo.getDataType() == MeteoDataType.NETCDF) {
            NetCDFDataInfo dinfo = (NetCDFDataInfo) this.dataInfo;
            try {
                dinfo.close();
            } catch (IOException ex) {
                Logger.getLogger(MeteoDataInfo.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    /**
     * Open GrADS data
     *
     * @param aFile Data file path
     */
    public void openGrADSData(String aFile) {
        dataInfo = new GrADSDataInfo();
        dataInfo.readDataInfo(aFile);
        _infoText = dataInfo.generateInfoText();
        GrADSDataInfo aDataInfo = (GrADSDataInfo) dataInfo;
        if (aDataInfo.DTYPE.equals("Gridded")) {
            yReserve = aDataInfo.OPTIONS.yrev;

            if (!aDataInfo.isLatLon) {
                IsLonLat = false;
                EarthWind = aDataInfo.EarthWind;
            }
        }
    }

    /**
     * Open ARL packed meteorological data
     *
     * @param aFile File path
     */
    public void openARLData(String aFile) {
        dataInfo = new ARLDataInfo();
        dataInfo.readDataInfo(aFile);
        IsLonLat = ((ARLDataInfo) dataInfo).isLatLon;

        //Get data info text
        _infoText = dataInfo.generateInfoText();
    }

    /**
     * Open AWX data
     *
     * @param aFile File path
     */
    public void openAWXData(String aFile) {
        AWXDataInfo aDataInfo = new AWXDataInfo();
        aDataInfo.readDataInfo(aFile);
        dataInfo = aDataInfo;
        //IsLonLat = aDataInfo.isLatLon;

        //Get data info text
        _infoText = aDataInfo.generateInfoText();
        if (aDataInfo.getProductType() == 4) {
            _meteoUVSet.setUV(false);
            _meteoUVSet.setFixUVStr(true);
            _meteoUVSet.setUStr("WindDirection");
            _meteoUVSet.setVStr("WindSpeed");
        }
    }

    /**
     * Open SYNOP data
     *
     * @param aFile File path
     * @param stFile Station file name
     */
    public void openSYNOPData(String aFile, String stFile) {
        SYNOPDataInfo aDataInfo = new SYNOPDataInfo();
        aDataInfo.setStationFileName(stFile);
        aDataInfo.readDataInfo(aFile);
        dataInfo = aDataInfo;
        //IsLonLat = aDataInfo.isLatLon;

        //Get data info text
        _infoText = aDataInfo.generateInfoText();
        _meteoUVSet.setUV(false);
        _meteoUVSet.setFixUVStr(true);
        _meteoUVSet.setUStr("WindDirection");
        _meteoUVSet.setVStr("WindSpeed");
    }

    /**
     * Open SYNOP data
     *
     * @param aFile File path
     * @param stFile Station file name
     */
    public void openMETARData(String aFile, String stFile) {
        METARDataInfo aDataInfo = new METARDataInfo();
        aDataInfo.setStationFileName(stFile);
        aDataInfo.readDataInfo(aFile);
        dataInfo = aDataInfo;
        //IsLonLat = aDataInfo.isLatLon;

        //Get data info text
        _infoText = aDataInfo.generateInfoText();
        _meteoUVSet.setUV(false);
        _meteoUVSet.setFixUVStr(true);
        _meteoUVSet.setUStr("WindDirection");
        _meteoUVSet.setVStr("WindSpeed");
    }

    /**
     * Open ASCII grid data
     *
     * @param aFile File path
     */
    public void openASCIIGridData(String aFile) {
        ASCIIGridDataInfo aDataInfo = new ASCIIGridDataInfo();
        aDataInfo.readDataInfo(aFile);
        dataInfo = aDataInfo;
        //ProjInfo = aDataInfo.projInfo;
        //IsLonLat = aDataInfo.isLatLon;

        //Get data info text
        _infoText = aDataInfo.generateInfoText();
    }

    /**
     * Open Geotiff grid data
     *
     * @param aFile File path
     */
    public void openGeoTiffData(String aFile) {
        GeoTiffDataInfo aDataInfo = new GeoTiffDataInfo();
        aDataInfo.readDataInfo(aFile);
        dataInfo = aDataInfo;

        //Get data info text
        _infoText = aDataInfo.generateInfoText();
    }
    
    /**
     * Open BIL grid data
     *
     * @param aFile File path
     */
    public void openBILData(String aFile) {
        BILDataInfo aDataInfo = new BILDataInfo();
        aDataInfo.readDataInfo(aFile);
        dataInfo = aDataInfo;

        //Get data info text
        _infoText = aDataInfo.generateInfoText();
    }

    /**
     * Open HYSPLIT concentration grid data
     *
     * @param aFile File path
     */
    public void openHYSPLITConcData(String aFile) {
        HYSPLITConcDataInfo aDataInfo = new HYSPLITConcDataInfo();
        aDataInfo.readDataInfo(aFile);
        dataInfo = aDataInfo;
        //ProjInfo = aDataInfo.projInfo;
        //IsLonLat = aDataInfo.isLatLon;

        //Get data info text
        _infoText = aDataInfo.generateInfoText();
    }
    
    /**
     * Open HYSPLIT concentration grid data
     *
     * @param aFile File path
     * @param bigendian Big endian or not
     */
    public void openHYSPLITConcData(String aFile, boolean bigendian) {
        HYSPLITConcDataInfo aDataInfo = new HYSPLITConcDataInfo(bigendian);
        aDataInfo.readDataInfo(aFile);
        dataInfo = aDataInfo;
        //ProjInfo = aDataInfo.projInfo;
        //IsLonLat = aDataInfo.isLatLon;

        //Get data info text
        _infoText = aDataInfo.generateInfoText();
    }

    /**
     * Open HYSPLIT trajectory data
     *
     * @param aFile File path
     */
    public void openHYSPLITTrajData(String aFile) {
        //Read data info                            
        HYSPLITTrajDataInfo aDataInfo = new HYSPLITTrajDataInfo();
        aDataInfo.readDataInfo(aFile);
        dataInfo = aDataInfo;
        _infoText = aDataInfo.generateInfoText();
    }

    /**
     * Open HYSPLIT particle data
     *
     * @param fileName File path
     */
    public void openHYSPLITPartData(String fileName) {
        //Read data info                            
        HYSPLITPartDataInfo aDataInfo = new HYSPLITPartDataInfo();
        aDataInfo.readDataInfo(fileName);
        dataInfo = aDataInfo;
        _infoText = aDataInfo.generateInfoText();
    }

    /**
     * Open HYSPLIT particle data
     *
     * @param fileName File path
     * @param skipNBytes Number of skip bytes
     */
    public void openHYSPLITPartData(String fileName, int skipNBytes) {
        //Read data info
        HYSPLITPartDataInfo aDataInfo = new HYSPLITPartDataInfo();
        aDataInfo.setSkipNBytes(skipNBytes);
        aDataInfo.readDataInfo(fileName);
        dataInfo = aDataInfo;
        _infoText = aDataInfo.generateInfoText();
    }

    /**
     * Open NetCDF data
     *
     * @param fileName File path
     */
    public void openNetCDFData(String fileName) {
        openNetCDFData(fileName, false);
    }

/**
 * 打开 NetCDF 数据文件。
 *
 * @param fileName 文件路径
 * @param keepOpen 是否保持文件打开状态
 */
public void openNetCDFData(String fileName, boolean keepOpen) {
    // 创建 NetCDFDataInfo 对象并读取数据信息
    NetCDFDataInfo aDataInfo = new NetCDFDataInfo();
    aDataInfo.readDataInfo(fileName, keepOpen);

    // 将读取的数据信息保存到成员变量中
    dataInfo = aDataInfo;
    _infoText = aDataInfo.generateInfoText();
}

    
    /**
     * Open NetCDF data
     *
     * @param ncfile Netcdf file
     * @param keepOpen 是否保持文件打开
     */
    public void openNetCDFData(NetcdfFile ncfile, boolean keepOpen) {
        NetCDFDataInfo aDataInfo = new NetCDFDataInfo();
        aDataInfo.readDataInfo(ncfile, keepOpen);
        dataInfo = aDataInfo;
        _infoText = aDataInfo.generateInfoText();
    }
    
    /**
     * Open GRIB data by predifined version - for mixed GRIB-1 and GRIB-2 data file.
     *
     * @param fileName File path
     * @param version GRIB data version: 1 or 2.
     */
    public void openGRIBData(String fileName, int version) {
        NetCDFDataInfo aDataInfo = new NetCDFDataInfo();
        MeteoDataType mdt = MeteoDataType.GRIB2;
        if (version == 1)
            mdt = MeteoDataType.GRIB1;
        aDataInfo.readDataInfo(fileName, mdt);
        dataInfo = aDataInfo;
        _infoText = aDataInfo.generateInfoText();
    }

    /**
     * Open Lon/Lat station data
     *
     * @param fileName File path
     */
    public void openLonLatData(String fileName) {
        dataInfo = new LonLatStationDataInfo();
        dataInfo.readDataInfo(fileName);
        _infoText = dataInfo.generateInfoText();
    }

    /**
     * Open Surfer ASCII grid data
     *
     * @param fileName File path
     */
    public void openSurferGridData(String fileName) {
        dataInfo = new SurferGridDataInfo();
        dataInfo.readDataInfo(fileName);
        _infoText = dataInfo.generateInfoText();
    }

    /**
     * Open MM5 Output data
     *
     * @param fileName File path
     */
    public void openMM5Data(String fileName) {
        dataInfo = new MM5DataInfo();
        dataInfo.readDataInfo(fileName);
        _infoText = dataInfo.generateInfoText();
    }
    
    /**
     * Open MM5 Output data
     *
     * @param fileName The MM5 output data file without big head
     * @param bigHeadFile The MM5 output data file with big head
     */
    public void openMM5Data(String fileName, String bigHeadFile) {
        dataInfo = new MM5DataInfo();
        ((MM5DataInfo)dataInfo).readDataInfo(fileName, bigHeadFile);
        _infoText = dataInfo.generateInfoText();
    }

    /**
     * Open MM5 Intermediate data
     *
     * @param fileName File path
     */
    public void openMM5IMData(String fileName) {
        dataInfo = new MM5IMDataInfo();
        dataInfo.readDataInfo(fileName);
        _infoText = dataInfo.generateInfoText();
    }

    /**
     * Open MatLab data file
     *
     * @param fileName File path
     */
    public void openMatLabData(String fileName) {
        dataInfo = new MatLabDataInfo();
        dataInfo.readDataInfo(fileName);
        _infoText = dataInfo.generateInfoText();
    }

    /**
     * Open MICAPS data
     *
     * @param fileName File name
     */
    public void openMICAPSData(String fileName) {
        MeteoDataType mdType = MICAPSDataInfo.getDataType(fileName);
        if (mdType == null) {
            return;
        }

        switch (mdType) {
            case MICAPS_1:
                dataInfo = new MICAPS1DataInfo();
                _meteoUVSet.setUV(false);
                _meteoUVSet.setFixUVStr(true);
                _meteoUVSet.setUStr("WindDirection");
                _meteoUVSet.setVStr("WindSpeed");
                break;
            case MICAPS_2:
                dataInfo = new MICAPS2DataInfo();
                _meteoUVSet.setUV(false);
                _meteoUVSet.setFixUVStr(true);
                _meteoUVSet.setUStr("WindDirection");
                _meteoUVSet.setVStr("WindSpeed");
                break;
            case MICAPS_3:
                dataInfo = new MICAPS3DataInfo();
                _meteoUVSet.setUV(false);
                _meteoUVSet.setFixUVStr(true);
                _meteoUVSet.setUStr("WindDirection");
                _meteoUVSet.setVStr("WindSpeed");
                break;
            case MICAPS_4:
                dataInfo = new MICAPS4DataInfo();
                break;
            case MICAPS_7:
                dataInfo = new MICAPS7DataInfo();
                break;
            case MICAPS_11:
                dataInfo = new MICAPS11DataInfo();
                break;
            case MICAPS_13:
                dataInfo = new MICAPS13DataInfo();
                break;
            case MICAPS_120:
                dataInfo = new MICAPS120DataInfo();
                break;
            case MICAPS_131:
                dataInfo = new MICAPS131DataInfo();
                break;
            case MICAPS_MDFS:
                dataInfo = new MDFSDataInfo();
                break;
        }
        dataInfo.readDataInfo(fileName);
        _infoText = dataInfo.generateInfoText();
        if (mdType == MeteoDataType.MICAPS_MDFS) {
            switch (((MDFSDataInfo)dataInfo).getType()) {
                case 1:
                case 2:
                    _meteoUVSet.setUV(false);
                    _meteoUVSet.setFixUVStr(true);
                    _meteoUVSet.setUStr("WindDirection");
                    _meteoUVSet.setVStr("WindSpeed");
                    break;
            }
        }
    }
    // </editor-fold>

    // <editor-fold desc="Get Data">
    /**
     * Get file name
     *
     * @return File name
     */
    public String getFileName() {
        return dataInfo.getFileName();
    }

    /**
     * Read array data of the variable
     *
     * @param varName Variable name
     * @return Array data
     */
    public Array read(String varName) {
        return this.dataInfo.read(varName);
    }

    /**
     * Read array data of the variable
     *
     * @param varName Variable name
     * @param origin The origin array
     * @param size The size array
     * @param stride The stride array
     * @return Array data
     */
    public Array read(String varName, int[] origin, int[] size, int[] stride) {
        return this.dataInfo.read(varName, origin, size, stride);
    }
    
    /**
     * Read array data from a variable
     * @param varName Variable name
     * @param ranges List of dimension ranges
     * @return Array data
     */
    public Array read(String varName, List<Range> ranges){
        int n = ranges.size();
        int[] origin = new int[n];
        int[] size = new int[n];
        int[] stride = new int[n];
        for (int i = 0; i < n; i++) {
            origin[i] = ranges.get(i).first();
            size[i] = ranges.get(i).last() - ranges.get(i).first() + 1;
            stride[i] = ranges.get(i).stride();
        }
        
        return read(varName, origin, size, stride);
    }

    /**
     * Read array data of the variable
     *
     * @param varName Variable name
     * @param origin The origin array
     * @param size The size array
     * @param stride The stride array
     * @return Array data
     */
    public Array read(String varName, List<Integer> origin, List<Integer> size, List<Integer> stride) {
        int n = origin.size();
        int[] origin_a = new int[n];
        int[] size_a = new int[n];
        int[] stride_a = new int[n];
        for (int i = 0; i < n; i++) {
            origin_a[i] = origin.get(i);
            size_a[i] = size.get(i);
        }
        if (stride == null) {
            for (int i = 0; i < n; i++) {
                stride_a[i] = 1;
            }
        } else {
            for (int i = 0; i < n; i++) {
                stride_a[i] = stride.get(i);
            }
        }

        return this.dataInfo.read(varName, origin_a, size_a, stride_a);
    }

    /**
     * Read array data of the variable
     *
     * @param varName Variable name
     * @param origin The origin array
     * @param size The size array
     * @return Array data
     */
    public Array read(String varName, List<Integer> origin, List<Integer> size) {
        return this.read(varName, origin, size, null);
    }
    
    /**
     * Take array data from the variable
     * @param varName Variable name
     * @param ranges Range list
     * @return Array data
     * @throws InvalidRangeException 
     */
    public Array take(String varName, List<Object> ranges) throws InvalidRangeException{
        int n = ranges.size();
        List<Range> nranges = new ArrayList<>();
        List<Object> branges = new ArrayList<>();
        for (int i = 0; i < n; i++){
            if (ranges.get(i) instanceof Range){
                nranges.add((Range)ranges.get(i));
                branges.add(new Range(0, ((Range)ranges.get(i)).length() - 1, 1));
            } else {
                List<Integer> list = (List<Integer>)ranges.get(i);
                int min = list.get(0);
                int max = min;
                if (list.size() > 1){
                    for (int j = 1; j < list.size(); j++){
                        if (min > list.get(j))
                            min = list.get(j);
                        if (max < list.get(j))
                            max = list.get(j);
                    }
                }
                Range range = new Range(min, max, 1);
                nranges.add(range);
                List<Integer> nlist = new ArrayList<>();
                for (int j = 0; j < list.size(); j++){
                    nlist.add(list.get(j) - min);
                }
                branges.add(nlist);
            }
        }
        
        Array r = read(varName, nranges);
        r = ArrayMath.take(r, branges);
        
        return r;
    }

    /**
     * Get grid data
     *
     * @param varName Variable name
     * @return Grid data
     */
    public GridData getGridData(String varName) {
        this.varName = varName;
        int varIdx = getVariableIndex(varName);
        if (varIdx < 0) {
            MathParser mathParser = new MathParser(this);
            try {
                GridData gridData = (GridData) mathParser.evaluate(varName);
                gridData.setProjInfo(this.getProjectionInfo());
                return gridData;
            } catch (ParseException | IOException ex) {
                Logger.getLogger(MeteoDataInfo.class.getName()).log(Level.SEVERE, null, ex);
                return null;
            }
        } else {
            GridData gridData = this.getGridData();
            gridData.setProjInfo(this.getProjectionInfo());
            gridData.setFieldName(varName);
            return gridData;
        }
    }

    /**
     * Get grid data
     *
     * @return Grid data
     */
    public GridData getGridData() {
        GridData gdata = null;
        switch (_dimensionSet) {
            case Lat_Lon:
                gdata = ((IGridDataInfo) dataInfo).getGridData_LonLat(_timeIdx, varName, _levelIdx);
                break;
            case Time_Lon:
                gdata = ((IGridDataInfo) dataInfo).getGridData_TimeLon(_latIdx, varName, _levelIdx);
                break;
            case Time_Lat:
                gdata = ((IGridDataInfo) dataInfo).getGridData_TimeLat(_lonIdx, varName, _levelIdx);
                break;
            case Level_Lon:
                gdata = ((IGridDataInfo) dataInfo).getGridData_LevelLon(_latIdx, varName, _timeIdx);
                break;
            case Level_Lat:
                gdata = ((IGridDataInfo) dataInfo).getGridData_LevelLat(_lonIdx, varName, _timeIdx);
                break;
            case Level_Time:
                gdata = ((IGridDataInfo) dataInfo).getGridData_LevelTime(_latIdx, varName, _lonIdx);
                break;
            case Lat:
                gdata = ((IGridDataInfo) dataInfo).getGridData_Lat(_timeIdx, _lonIdx, varName, _levelIdx);
                break;
            case Level:
                gdata = ((IGridDataInfo) dataInfo).getGridData_Level(_lonIdx, _latIdx, varName, _timeIdx);
                break;
            case Lon:
                gdata = ((IGridDataInfo) dataInfo).getGridData_Lon(_timeIdx, _latIdx, varName, _levelIdx);
                break;
            case Time:
                gdata = ((IGridDataInfo) dataInfo).getGridData_Time(_lonIdx, _latIdx, varName, _levelIdx);
                break;
        }

        if (gdata != null) {
            gdata.setProjInfo(this.getProjectionInfo());
        }

        return gdata;
    }

    /**
     * Get station data
     *
     * @param varName Variable name
     * @return Station data
     */
    public StationData getStationData(String varName) {
        this.varName = varName;
        int varIdx = getVariableIndex(varName);
        if (varIdx >= 0) {
            return this.getStationData();
        } else {
            MathParser mathParser = new MathParser(this);
            try {
                StationData stationData = (StationData) mathParser.evaluate(varName);
                stationData.projInfo = this.getProjectionInfo();
                return stationData;
            } catch (ParseException ex) {
                Logger.getLogger(MeteoDataInfo.class.getName()).log(Level.SEVERE, null, ex);
                return null;
            } catch (IOException ex) {
                Logger.getLogger(MeteoDataInfo.class.getName()).log(Level.SEVERE, null, ex);
                return null;
            }
        }
    }

    /**
     * Get station data
     *
     * @return Station data
     */
    public StationData getStationData() {
        StationData stData = ((IStationDataInfo) dataInfo).getStationData(_timeIdx, varName, _levelIdx);
        stData.projInfo = this.getProjectionInfo();
        return stData;
    }

    /**
     * Get station model data
     *
     * @return Station model data
     */
    public StationModelData getStationModelData() {
        return ((IStationDataInfo) dataInfo).getStationModelData(_timeIdx, _levelIdx);
    }

    /**
     * Get station info data
     *
     * @return Station info data
     */
    public StationInfoData getStationInfoData() {
        return ((IStationDataInfo) dataInfo).getStationInfoData(_timeIdx, _levelIdx);
    }

    /**
     * Get station info data
     *
     * @param timeIndex Time index
     * @return Station info data
     */
    public StationInfoData getStationInfoData(int timeIndex) {
        return ((IStationDataInfo) dataInfo).getStationInfoData(timeIndex, _levelIdx);
    }

    /**
     * Get variable index
     *
     * @param varName Variable name
     * @return Variable index
     */
    public int getVariableIndex(String varName) {
        List<String> varList = dataInfo.getVariableNames();
        int idx = varList.indexOf(varName);

        return idx;
    }

    /**
     * Get time of arrial grid data - the time after the start of the simulation
     * that the concentration exceeds the given threshold concentration
     *
     * @param varName Variable name
     * @param threshold Threshold value
     * @return Time of arrial grid data
     */
    public GridData getArrivalTimeData(String varName, double threshold) {
        int tnum = this.getDataInfo().getTimeNum();
        this.setTimeIndex(0);
        GridData gData = this.getGridData(varName);
        GridData tData = new GridData(gData);
        //tData.missingValue = -9999.0;
        tData = tData.setValue(tData.getDoubleMissingValue());
        int xnum = gData.getXNum();
        int ynum = gData.getYNum();
        LocalDateTime date = this.getDataInfo().getTimes().get(0);
        List<Integer> hours = this.getDataInfo().getTimeValues(date, "hours");
        for (int t = 0; t < tnum; t++) {
            int hour = hours.get(t);
            if (t >= 1) {
                this.setTimeIndex(t);
                gData = this.getGridData(varName);
            }
            for (int i = 0; i < ynum; i++) {
                for (int j = 0; j < xnum; j++) {
                    if (gData.getData()[i][j] >= threshold) {
                        if (MIMath.doubleEquals(tData.getDoubleValue(i, j), tData.getDoubleMissingValue())) {
                            tData.setValue(i, j, hour);
                        }
                    }
                }
            }
        }

        return tData;
    }

    /**
     * Interpolate data to a station point
     *
     * @param varName Variable name
     * @param x X coordinate of the station
     * @param y Y coordinate of the station
     * @param z Z coordinate of the station
     * @param t Time coordinate of the station
     * @return Interpolated value
     */
    public double toStation(String varName, double x, double y, double z, LocalDateTime t) {
        List<LocalDateTime> times = this.getDataInfo().getTimes();
        int tnum = times.size();
        if (t.isBefore(times.get(0)) || t.isAfter(times.get(tnum - 1))) {
            return this.getDataInfo().getMissingValue();
        }

        double ivalue = this.getDataInfo().getMissingValue();
        double v_t1, v_t2;
        for (int i = 0; i < tnum; i++) {
            if (t.equals(times.get(i))) {
                ivalue = this.toStation(varName, x, y, z, i);
                break;
            }
            if (t.isBefore(times.get(i))) {
                v_t1 = this.toStation(varName, x, y, z, i - 1);
                v_t2 = this.toStation(varName, x, y, z, i);
                int h = (int)Duration.between(times.get(i - 1), t).toHours();
                int th = (int)Duration.between(times.get(i - 1), times.get(i)).toHours();
                ivalue = (v_t2 - v_t1) * h / th + v_t1;
                break;
            }
        }

        return ivalue;
    }

    /**
     * Interpolate data to a station point
     *
     * @param varName Variable name
     * @param x X coordinate of the station
     * @param y Y coordinate of the station
     * @param t Time coordinate of the station
     * @return Interpolated value
     */
    public double toStation(String varName, double x, double y, LocalDateTime t) {
        List<LocalDateTime> times = this.getDataInfo().getTimes();
        int tnum = times.size();
        if (t.isBefore(times.get(0)) || t.isAfter(times.get(tnum - 1))) {
            return this.getDataInfo().getMissingValue();
        }

        double ivalue = this.getDataInfo().getMissingValue();
        double v_t1, v_t2;
        for (int i = 0; i < tnum; i++) {
            if (t.equals(times.get(i))) {
                ivalue = this.toStation(varName, x, y, i);
                break;
            }
            if (t.isBefore(times.get(i))) {
                v_t1 = this.toStation(varName, x, y, i - 1);
                v_t2 = this.toStation(varName, x, y, i);
                int h = (int)Duration.between(times.get(i - 1), t).toHours();
                int th = (int)Duration.between(times.get(i - 1), times.get(i)).toHours();
                ivalue = (v_t2 - v_t1) * h / th + v_t1;
                break;
            }
        }

        return ivalue;
    }

    /**
     * Interpolate data to a station point
     *
     * @param varNames Variable names
     * @param x X coordinate of the station
     * @param y Y coordinate of the station
     * @param z Z coordinate of the station
     * @param t Time coordinate of the station
     * @return Interpolated values
     */
    public List<Double> toStation(List<String> varNames, double x, double y, double z, LocalDateTime t) {
        List<LocalDateTime> times = this.getDataInfo().getTimes();
        int tnum = times.size();
        if (t.isBefore(times.get(0)) || t.isAfter(times.get(tnum - 1))) {
            return null;
        }

        List<Double> ivalues = new ArrayList<>();
        double v_t1, v_t2;
        List<Double> v_t1s, v_t2s;
        for (int i = 0; i < tnum; i++) {
            if (t.equals(times.get(i))) {
                ivalues = this.toStation(varNames, x, y, z, i);
                break;
            }
            if (t.isBefore(times.get(i))) {
                v_t1s = this.toStation(varNames, x, y, z, i - 1);
                v_t2s = this.toStation(varNames, x, y, z, i);
                int h = (int)Duration.between(times.get(i - 1), t).toHours();
                int th = (int)Duration.between(times.get(i - 1), times.get(i)).toHours();
                for (int j = 0; j < v_t1s.size(); j++) {
                    v_t1 = v_t1s.get(j);
                    v_t2 = v_t2s.get(j);
                    ivalues.add((v_t2 - v_t1) * h / th + v_t1);
                }
                break;
            }
        }

        return ivalues;
    }

    /**
     * Interpolate data to a station point
     *
     * @param varName Variable name
     * @param x X coordinate of the station
     * @param y Y coordinate of the station
     * @param z Z coordinate of the station
     * @param tidx Time index
     * @return Interpolated value
     */
    public double toStation(String varName, double x, double y, double z, int tidx) {
        double ivalue = this.getDataInfo().getMissingValue();
        Variable var = this.getDataInfo().getVariable(varName);
        List<Double> levels = var.getZDimension().getDimValueList();
        int znum = levels.size();
        double v_z1, v_z2;
        this.setTimeIndex(tidx);
        if (levels.get(1) - levels.get(0) > 0) {
            for (int j = 0; j < znum; j++) {
                if (MIMath.doubleEquals(z, levels.get(j))) {
                    this.setLevelIndex(j);
                    ivalue = this.getGridData(varName).toStation(x, y);
                    break;
                }
                if (z < levels.get(j)) {
                    if (j == 0) {
                        j = 1;
                    }
                    this.setLevelIndex(j - 1);
                    v_z1 = this.getGridData(varName).toStation(x, y);
                    this.setLevelIndex(j);
                    v_z2 = this.getGridData(varName).toStation(x, y);
                    ivalue = (v_z2 - v_z1) * (z - levels.get(j - 1)) / (levels.get(j) - levels.get(j - 1)) + v_z1;
                    break;
                }
            }
        } else {
            for (int j = 0; j < znum; j++) {
                if (MIMath.doubleEquals(z, levels.get(j))) {
                    this.setLevelIndex(j);
                    ivalue = this.getGridData(varName).toStation(x, y);
                    break;
                }
                if (z > levels.get(j)) {
                    if (j == 0) {
                        j = 1;
                    }
                    this.setLevelIndex(j - 1);
                    v_z1 = this.getGridData(varName).toStation(x, y);
                    this.setLevelIndex(j);
                    v_z2 = this.getGridData(varName).toStation(x, y);
                    ivalue = (v_z2 - v_z1) * (z - levels.get(j - 1)) / (levels.get(j) - levels.get(j - 1)) + v_z1;
                    break;
                }
            }
        }

        return ivalue;
    }

    /**
     * Interpolate data to a station point
     *
     * @param varName Variable name
     * @param x X coordinate of the station
     * @param y Y coordinate of the station
     * @param tidx Time index
     * @return Interpolated value
     */
    public double toStation(String varName, double x, double y, int tidx) {
        this.setTimeIndex(tidx);
        this.setLevelIndex(0);
        double ivalue = this.getGridData(varName).toStation(x, y);

        return ivalue;
    }

    /**
     * Interpolate data to a station point
     *
     * @param varNames Variable names
     * @param x X coordinate of the station
     * @param y Y coordinate of the station
     * @param z Z coordinate of the station
     * @param tidx Time index
     * @return Interpolated values
     */
    public List<Double> toStation(List<String> varNames, double x, double y, double z, int tidx) {
        List<Double> ivalues = new ArrayList<>();
        double ivalue;
        Variable var = this.getDataInfo().getVariable(varNames.get(0));
        List<Double> levels = var.getZDimension().getDimValueList();
        int znum = levels.size();
        double v_z1, v_z2;
        this.setTimeIndex(tidx);
        for (int j = 0; j < znum; j++) {
            for (String varName : varNames) {
                if (MIMath.doubleEquals(z, levels.get(j))) {
                    this.setLevelIndex(j);
                    ivalue = this.getGridData(varName).toStation(x, y);
                    ivalues.add(ivalue);
                    break;
                }
                if (z < levels.get(j)) {
                    this.setLevelIndex(j - 1);
                    v_z1 = this.getGridData(varName).toStation(x, y);
                    this.setLevelIndex(j);
                    v_z2 = this.getGridData(varName).toStation(x, y);
                    ivalue = (v_z2 - v_z1) * (z - levels.get(j - 1)) / (levels.get(j) - levels.get(j - 1)) + v_z1;
                    ivalues.add(ivalue);
                    break;
                }
            }
        }

        return ivalues;
    }
    // </editor-fold>
    // <editor-fold desc="Others">
    @Override
    public String toString(){
        return new File(this.getFileName()).getName();
    }
    // </editor-fold>
    // </editor-fold>
}
