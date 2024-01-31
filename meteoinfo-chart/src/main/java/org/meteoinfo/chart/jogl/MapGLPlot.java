package org.meteoinfo.chart.jogl;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import org.joml.Vector3f;
import org.meteoinfo.chart.ChartText;
import org.meteoinfo.geometry.graphic.GraphicCollection3D;
import org.meteoinfo.chart.graphic.GraphicProjectionUtil;
import org.meteoinfo.chart.plot.MapGridLine;
import org.meteoinfo.chart.plot.MapGridLine3D;
import org.meteoinfo.common.*;
import org.meteoinfo.geometry.graphic.Graphic;
import org.meteoinfo.projection.ProjectionInfo;

import java.awt.geom.Rectangle2D;
import java.util.List;

public class MapGLPlot extends GLPlot {

    /**
     * Constructor
     */
    public MapGLPlot() {
        this(ProjectionInfo.LONG_LAT);
    }

    /**
     * Constructor
     * @param projInfo Projection info
     */
    public MapGLPlot(ProjectionInfo projInfo) {
        super();
        this.gridLine = new MapGridLine3D();
        this.projInfo = projInfo;
        updateExtent();
    }

    /**
     * Get projection info
     * @return Projection info
     */
    public ProjectionInfo getProjInfo() {
        return this.projInfo;
    }

    /**
     * Set projection info
     * @param value Projection info
     */
    public void setProjInfo(ProjectionInfo value) {
        this.projInfo = value;
        if (!projInfo.isLonLat())
            ((MapGridLine3D) this.gridLine).setProjInfo(projInfo);
    }

    /**
     * Add a graphic
     *
     * @param graphic The graphic
     * @param proj The graphic projection
     */
    @Override
    public void addGraphic(Graphic graphic, ProjectionInfo proj) {
        if (proj.equals(this.projInfo)) {
            super.addGraphic(graphic);
        } else {
            Graphic nGraphic = GraphicProjectionUtil.projectClipGraphic(graphic, proj, this.projInfo);
            if (nGraphic instanceof GraphicCollection3D) {
                ((GraphicCollection3D) nGraphic).setUsingLight(((GraphicCollection3D) graphic).isUsingLight());
            }
            super.addGraphic(nGraphic);
        }
    }

    /**
     * Add a graphic
     *
     * @param index The index
     * @param graphic The graphic
     * @param proj The graphic projection
     */
    @Override
    public void addGraphic(int index, Graphic graphic, ProjectionInfo proj) {
        if (proj.equals(this.projInfo)) {
            super.addGraphic(index, graphic);
        } else {
            Graphic nGraphic = GraphicProjectionUtil.projectClipGraphic(graphic, proj, this.projInfo);
            super.addGraphic(index, nGraphic);
        }
    }

    /**
     * Set draw extent
     *
     * @param value Extent
     */
    @Override
    public void setDrawExtent(Extent value) {
        super.setDrawExtent(value);

        if (!this.projInfo.isLonLat()) {
            ((MapGridLine3D) this.gridLine).setExtent(value);
        }
    }

    @Override
    protected void updateExtent() {
        super.updateExtent();
        if (!this.projInfo.isLonLat()) {
            ((MapGridLine3D) this.gridLine).setExtent(this.drawExtent);
        }
    }

    @Override
    protected void drawXYGridLine(GL2 gl) {
        if (this.projInfo.isLonLat()) {
            super.drawXYGridLine(gl);
        } else {
            MapGridLine mapGridLine = (MapGridLine3D) gridLine;
            //Longitude
            if (mapGridLine.isDrawXLine()) {
                if (mapGridLine.getLongitudeLines() != null) {
                    this.drawGraphics(gl, mapGridLine.getLongitudeLines());
                }
            }
            //Latitude
            if (mapGridLine.isDrawYLine()) {
                if (mapGridLine.getLatitudeLines() != null) {
                    this.drawGraphics(gl, mapGridLine.getLatitudeLines());
                }
            }
            if (this.lighting.isStarted()) {
                this.lighting.stop(gl);
            }
        }
    }

    @Override
    protected void drawAxis(GL2 gl) {
        if (this.projInfo.isLonLat()) {
            super.drawAxis(gl);
        } else {
            float xMin, xMax, yMin, yMax, zMin, zMax;
            xMin = (float) axesExtent.minX;
            xMax = (float) axesExtent.maxX;
            yMin = (float) axesExtent.minY;
            yMax = (float) axesExtent.maxY;
            zMin = (float) axesExtent.minZ;
            zMax = (float) axesExtent.maxZ;
            /*xMin = this.transform.transform_x((float) axesExtent.minX);
            xMax = this.transform.transform_x((float) axesExtent.maxX);
            yMin = this.transform.transform_y((float) axesExtent.minY);
            yMax = this.transform.transform_y((float) axesExtent.maxY);
            zMin = this.transform.transform_z((float) axesExtent.minZ);
            zMax = this.transform.transform_z((float) axesExtent.maxZ);*/

            gl.glDepthFunc(GL.GL_ALWAYS);

            //Draw axis
            float[] rgba;
            float x, y;
            int skip;
            XAlign xAlign;
            YAlign yAlign;
            Rectangle2D rect;
            Vector3f center = this.transform.getCenter();
            float strWidth, strHeight;
            MapGridLine3D mapGridLine = (MapGridLine3D) gridLine;
            if (this.displayXY) {
                //Draw x/y axis lines
                //x axis line
                if (this.angleY >= 90 && this.angleY < 270) {
                    y = yMax;
                } else {
                    y = yMin;
                }
                rgba = this.xAxis.getLineColor().getRGBComponents(null);
                gl.glColor4f(rgba[0], rgba[1], rgba[2], rgba[3]);
                gl.glLineWidth(this.xAxis.getLineWidth() * this.dpiScale);
                gl.glBegin(GL2.GL_LINES);
                gl.glVertex3f(xMin, y, zMin);
                gl.glVertex3f(xMax, y, zMin);
                gl.glEnd();

                //Longitude axis ticks
                float tickLen = this.xAxis.getTickLength() * this.lenScale * transform.getYLength() / 2;
                float axisLen = this.toScreenLength(xMin, y, zMin, xMax, y, zMin);
                float y1 = y > center.y ? y + tickLen : y - tickLen;
                if (this.angleY < 90 || (this.angleY >= 180 && this.angleY < 270)) {
                    xAlign = XAlign.LEFT;
                } else {
                    xAlign = XAlign.RIGHT;
                }
                if (this.angleX > -120) {
                    yAlign = YAlign.TOP;
                } else {
                    yAlign = YAlign.BOTTOM;
                }
                strWidth = 0.0f;
                strHeight = 0.0f;
                List<GridLabel> lonLabels = mapGridLine.getLongitudeLabels();
                for (int i = 0; i < lonLabels.size(); i++) {
                    GridLabel gridLabel = lonLabels.get(i);
                    PointD point = gridLabel.getCoord();
                    x = (float) point.X;
                    if (x < axesExtent.minX || x > axesExtent.maxX) {
                        continue;
                    }
                    //x = this.transform.transform_x(x);

                    //Draw tick line
                    rgba = this.xAxis.getLineColor().getRGBComponents(null);
                    gl.glColor4f(rgba[0], rgba[1], rgba[2], rgba[3]);
                    gl.glLineWidth(this.xAxis.getLineWidth() * this.dpiScale);
                    gl.glBegin(GL2.GL_LINES);
                    gl.glVertex3f(x, y, zMin);
                    gl.glVertex3f(x, y1, zMin);
                    gl.glEnd();

                    //Draw tick label
                    rect = drawString(gl, gridLabel.getLabString(), this.xAxis.getTickLabelFont(),
                            this.xAxis.getTickLabelColor(), x, y1, zMin, xAlign, yAlign);
                    if (strWidth < rect.getWidth()) {
                        strWidth = (float) rect.getWidth();
                    }
                    if (strHeight < rect.getHeight()) {
                        strHeight = (float) rect.getHeight();
                    }
                }

                //Draw x axis label
                ChartText label = this.xAxis.getLabel();
                if (label != null) {
                    this.updateTextRender(label.getFont());
                    strWidth += this.tickSpace;
                    float angle = this.toScreenAngle(xMin, y, zMin, xMax, y, zMin);
                    angle = y < center.y ? 270 - angle : 90 - angle;
                    float yShift = Math.min(-strWidth, -strWidth);
                    if (this.angleX <= -120) {
                        yShift = -yShift;
                    }
                    float x1 = (xMin + xMax) / 2;
                    drawString(gl, label, x1, y1, zMin, XAlign.CENTER, yAlign, angle, 0, yShift);
                }

                ////////////////////////////////////////////
                //y axis line
                if (this.angleY >= 180 && this.angleY < 360) {
                    x = xMax;
                } else {
                    x = xMin;
                }
                rgba = this.yAxis.getLineColor().getRGBComponents(null);
                gl.glColor4f(rgba[0], rgba[1], rgba[2], rgba[3]);
                gl.glLineWidth(this.yAxis.getLineWidth() * this.dpiScale);
                gl.glBegin(GL2.GL_LINES);
                gl.glVertex3f(x, yMin, zMin);
                gl.glVertex3f(x, yMax, zMin);
                gl.glEnd();

                //y axis ticks
                axisLen = this.toScreenLength(x, yMin, zMin, x, yMax, zMin);
                tickLen = this.yAxis.getTickLength() * this.lenScale * transform.getXLength() / 2;
                float x1 = x > center.x ? x + tickLen : x - tickLen;
                if (this.angleY < 90 || (this.angleY >= 180 && this.angleY < 270)) {
                    xAlign = XAlign.RIGHT;
                } else {
                    xAlign = XAlign.LEFT;
                }
                if (this.angleX > -120) {
                    yAlign = YAlign.TOP;
                } else {
                    yAlign = YAlign.BOTTOM;
                }
                strWidth = 0.0f;
                strHeight = 0.0f;
                List<GridLabel> latLabels = mapGridLine.getLatitudeLabels();
                for (int i = 0; i < latLabels.size(); i++) {
                    GridLabel gridLabel = latLabels.get(i);
                    PointD point = gridLabel.getCoord();
                    y = (float) point.Y;
                    if (y < axesExtent.minY || y > axesExtent.maxY) {
                        continue;
                    }
                    //y = this.transform.transform_y(y);

                    //Draw tick line
                    rgba = this.yAxis.getLineColor().getRGBComponents(null);
                    gl.glColor4f(rgba[0], rgba[1], rgba[2], rgba[3]);
                    gl.glLineWidth(this.yAxis.getLineWidth() * this.dpiScale);
                    gl.glBegin(GL2.GL_LINES);
                    gl.glVertex3f(x, y, zMin);
                    gl.glVertex3f(x1, y, zMin);
                    gl.glEnd();

                    //Draw tick label
                    rect = drawString(gl, gridLabel.getLabString(), this.yAxis.getTickLabelFont(),
                            this.yAxis.getTickLabelColor(), x1, y, zMin, xAlign, yAlign);
                    if (strWidth < rect.getWidth()) {
                        strWidth = (float) rect.getWidth();
                    }
                    if (strHeight < rect.getHeight()) {
                        strHeight = (float) rect.getHeight();
                    }
                }

                //Draw y axis label
                label = this.yAxis.getLabel();
                if (label != null) {
                    this.updateTextRender(label.getFont());
                    strWidth += this.tickSpace;
                    float angle = this.toScreenAngle(x, yMin, zMin, x, yMax, zMin);
                    angle = x > center.x ? 270 - angle : 90 - angle;
                    float yShift = Math.min(-strWidth, -strWidth);
                    if (this.angleX <= -120) {
                        yShift = -yShift;
                    }
                    y1 = (yMin + yMax) / 2;
                    drawString(gl, label, x1, y1, zMin, XAlign.CENTER, yAlign, angle, 0, yShift);
                }
            }

            //Draw z axis
            if (this.displayZ) {
                //z axis line
                if (this.angleY < 90) {
                    x = xMin;
                    y = yMax;
                } else if (this.angleY < 180) {
                    x = xMax;
                    y = yMax;
                } else if (this.angleY < 270) {
                    x = xMax;
                    y = yMin;
                } else {
                    x = xMin;
                    y = yMin;
                }
                rgba = this.zAxis.getLineColor().getRGBComponents(null);
                gl.glColor4f(rgba[0], rgba[1], rgba[2], rgba[3]);
                gl.glLineWidth(this.zAxis.getLineWidth() * this.dpiScale);
                gl.glBegin(GL2.GL_LINES);
                gl.glVertex3f(x, y, zMin);
                gl.glVertex3f(x, y, zMax);
                gl.glEnd();

                //z axis ticks
                this.zAxis.updateTickLabels();
                List<ChartText> tlabs = this.zAxis.getTickLabels();
                float axisLen = this.toScreenLength(x, y, zMin, x, y, zMax);
                skip = getLabelGap(this.zAxis.getTickLabelFont(), tlabs, axisLen);
                float x1 = x;
                float y1 = y;
                float tickLen = this.zAxis.getTickLength() * this.lenScale * transform.getYLength() / 2;
                if (x < center.x) {
                    if (y > center.y) {
                        y1 += tickLen;
                    } else {
                        x1 -= tickLen;
                    }
                } else {
                    if (y > center.y) {
                        x1 += tickLen;
                    } else {
                        y1 -= tickLen;
                    }
                }
                xAlign = XAlign.RIGHT;
                yAlign = YAlign.CENTER;
                strWidth = 0.0f;
                float v;
                for (int i = 0; i < this.zAxis.getTickValues().length; i += skip) {
                    v = (float) this.zAxis.getTickValues()[i];
                    if (v < axesExtent.minZ || v > axesExtent.maxZ) {
                        continue;
                    }
                    //v = this.transform.transform_z(v);
                    if (i == tlabs.size()) {
                        break;
                    }

                    //Draw tick line
                    rgba = this.zAxis.getLineColor().getRGBComponents(null);
                    gl.glColor4f(rgba[0], rgba[1], rgba[2], rgba[3]);
                    gl.glLineWidth(this.zAxis.getLineWidth() * this.dpiScale);
                    gl.glBegin(GL2.GL_LINES);
                    gl.glVertex3f(x, y, v);
                    gl.glVertex3f(x1, y1, v);
                    gl.glEnd();

                    //Draw tick label
                    rect = drawString(gl, tlabs.get(i), x1, y1, v, xAlign, yAlign, -this.tickSpace, 0);
                    if (strWidth < rect.getWidth()) {
                        strWidth = (float) rect.getWidth();
                    }
                }

                //Draw z axis label
                ChartText label = this.zAxis.getLabel();
                if (label != null) {
                    float yShift = strWidth + this.tickSpace * 3;
                    float z1 = (zMax + zMin) * 0.5f;
                    drawString(gl, label, x1, y1, z1, XAlign.CENTER, YAlign.BOTTOM, 90.f, 0, yShift);
                }
            }
            gl.glDepthFunc(GL2.GL_LEQUAL);
        }
    }
}
