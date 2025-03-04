/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.meteoinfo.data.mapdata.webmap;

import java.awt.*;

/**
 *
 * @author Yaqiang Wang
 */
public interface IWebMapPanel {
    /**
     * 获取 Web 地图图层缩放
     * @return Web map layer zoom
     */
    public abstract int getWebMapZoom();
    
    /**
     * 重新绘制功能
     */
    public abstract void reDraw();

    /**
     * 重新绘制功能
     * @param graphics2D Graphics2D object
     * @param width Width
     * @param height Height
     */
    public abstract void reDraw(Graphics2D graphics2D, int width, int height);
}
