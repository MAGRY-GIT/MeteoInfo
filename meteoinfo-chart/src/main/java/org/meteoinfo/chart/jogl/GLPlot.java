package org.meteoinfo.chart.jogl;

import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.*;
import com.jogamp.opengl.glu.GLU;
import com.jogamp.opengl.glu.GLUquadric;
import com.jogamp.opengl.glu.GLUtessellator;
import com.jogamp.opengl.util.awt.TextRenderer;
import com.jogamp.opengl.util.gl2.GLUT;
import com.jogamp.opengl.util.texture.Texture;
import com.jogamp.opengl.util.texture.awt.AWTTextureIO;
import org.joml.*;
import org.meteoinfo.chart.*;
import org.meteoinfo.chart.axis.Axis;
import org.meteoinfo.chart.graphic.*;
import org.meteoinfo.chart.graphic.pipe.Pipe;
import org.meteoinfo.chart.graphic.pipe.PipeShape;
import org.meteoinfo.chart.jogl.tessellator.Primitive;
import org.meteoinfo.chart.jogl.tessellator.TessPolygon;
import org.meteoinfo.chart.plot.GridLine;
import org.meteoinfo.chart.plot.Plot;
import org.meteoinfo.chart.plot.PlotType;
import org.meteoinfo.chart.render.jogl.*;
import org.meteoinfo.chart.shape.TextureShape;
import org.meteoinfo.common.*;
import org.meteoinfo.common.colors.ColorMap;
import org.meteoinfo.data.Dataset;
import org.meteoinfo.geo.drawing.Draw;
import org.meteoinfo.geo.drawing.StringType;
import org.meteoinfo.geometry.colors.BoundaryNorm;
import org.meteoinfo.geometry.colors.Normalize;
import org.meteoinfo.geometry.graphic.Graphic;
import org.meteoinfo.geometry.graphic.GraphicCollection;
import org.meteoinfo.geometry.graphic.GraphicCollection3D;
import org.meteoinfo.geometry.legend.*;
import org.meteoinfo.geometry.shape.*;
import org.meteoinfo.geometry.shape.Shape;
import org.meteoinfo.math.meteo.MeteoMath;
import org.meteoinfo.projection.ProjectionInfo;
import org.meteoinfo.projection.ProjectionUtil;
import org.scilab.forge.jlatexmath.TeXConstants;
import org.scilab.forge.jlatexmath.TeXFormula;
import org.scilab.forge.jlatexmath.TeXIcon;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.lang.Math;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.jogamp.opengl.GL.*;
import static com.jogamp.opengl.GL.GL_TRIANGLE_STRIP;
import static com.jogamp.opengl.GL2ES2.GL_TEXTURE_3D;
import static com.jogamp.opengl.GL2ES2.GL_TEXTURE_WRAP_R;
import static com.jogamp.opengl.GL2ES3.GL_TEXTURE_BASE_LEVEL;

public class GLPlot extends Plot {

    // <editor-fold desc="Variables">
    protected ProjectionInfo projInfo;
    protected boolean sampleBuffers = false;
    protected Color background = Color.white;
    protected boolean doScreenShot;
    protected BufferedImage screenImage;
    protected GL2 gl;
    protected GLU glu = new GLU();
    protected final GLUT glut = new GLUT();
    protected int startList = 2;
    protected GraphicCollection3D graphics;
    protected Extent3D graphicExtent;
    protected Extent3D drawExtent;
    protected Extent3D axesExtent;
    protected boolean fixExtent;
    protected TextRenderer textRenderer;
    protected ChartText title;
    protected GridLine gridLine;
    protected java.util.List<ChartLegend> legends;
    protected final Axis xAxis;
    protected final Axis yAxis;
    protected final Axis zAxis;
    protected java.util.List<ZAxisOption> zAxisLocations;
    protected Transform transform = new Transform();
    protected boolean clipPlane = true;
    protected boolean axesZoom = false;

    protected Color boxColor = Color.lightGray;

    protected boolean boxed, mesh, scaleBox, displayXY, displayZ,
            drawBoundingBox, hideOnDrag, drawBase;

    protected int viewport[] = new int[4];
    protected float mvmatrix[] = new float[16];
    protected float projmatrix[] = new float[16];
    protected Matrix4f modelViewMatrix = new Matrix4f();
    protected Matrix4f projectionMatrix = new Matrix4f();
    protected Matrix4f viewProjMatrix = new Matrix4f();
    protected Matrix4f modelViewMatrixR = new Matrix4f();

    protected float angleX;
    protected float angleY;
    protected float headAngle;
    protected float pitchAngle;
    protected AspectType aspectType = AspectType.AUTO;

    protected Plot3DGL.TessCallback tessCallback;
    protected int width;
    protected int height;
    protected float tickSpace = 5.0f;
    final protected float lenScale = 0.01f;
    protected Lighting lighting = new Lighting();
    protected boolean antialias;
    protected float dpiScale;   //DPI scale factor
    protected boolean orthographic;
    protected float distance;
    protected float fieldOfView;
    protected GLAutoDrawable drawable;

    protected Map<Graphic, JOGLGraphicRender> renderMap = new HashMap<>();
    protected boolean alwaysUpdateBuffers = false;

    // </editor-fold>
    // <editor-fold desc="Constructor">
    /**
     * Constructor
     */
    public GLPlot() {
        this.projInfo = null;
        this.doScreenShot = false;
        this.legends = new ArrayList<>();
        this.xAxis = new Axis();
        this.xAxis.setTickLength(8);
        this.yAxis = new Axis();
        this.yAxis.setTickLength(8);
        this.zAxis = new Axis();
        this.zAxis.setTickLength(8);
        this.zAxisLocations = new ArrayList<>();
        this.fixExtent = false;
        this.graphics = new GraphicCollection3D();
        this.hideOnDrag = false;
        this.boxed = true;
        this.gridLine = new GridLine(true);
        this.drawBase = true;
        this.displayXY = true;
        this.displayZ = true;
        this.drawBoundingBox = false;
        this.antialias = false;
        this.dpiScale = 1;
        this.orthographic = true;
        this.distance = 5.f;
        this.fieldOfView = 45.f;
        this.initAngles();
        this.graphicExtent = new Extent3D();
        Extent3D extent3D = new Extent3D(-1, 1, -1, 1, -1, 1);
        this.drawExtent = (Extent3D) extent3D.clone();
        this.transform.setExtent(this.drawExtent);
        this.setAxesExtent((Extent3D) this.drawExtent.clone());
    }

    /**
     * Initialize angles
     */
    public void initAngles() {
        this.angleX = -45.f;
        this.angleY = 45.f;
        this.headAngle = 0.f;
        this.pitchAngle = 0.f;
    }

    // </editor-fold>
    // <editor-fold desc="GetSet">
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
    }

    /**
     * Get is sample buffers or not
     * @return Boolean
     */
    public boolean isSampleBuffers() {
        return this.sampleBuffers;
    }

    /**
     * Set sample buffers or not
     * @param value Boolean
     */
    public void setSampleBuffers(boolean value) {
        this.sampleBuffers = value;
    }

    /**
     * Get background color
     * @return Background color
     */
    public Color getBackground() {
        return this.background;
    }

    /**
     * Set background color
     * @param value Background color
     */
    public void setBackground(Color value) {
        this.background = value;
        if (this.background == Color.black) {
            setForeground(Color.white);
        }
    }

    /**
     * Set foreground color
     * @param value Foreground color
     */
    public void setForeground(Color value) {
        this.boxColor = value;
        this.gridLine.setColor(value);
        this.xAxis.setColor_All(value);
        this.yAxis.setColor_All(value);
        this.zAxis.setColor_All(value);
    }

    /**
     * Get graphics
     * @return The graphics
     */
    public GraphicCollection3D getGraphics() {
        return this.graphics;
    }

    /**
     * Set graphics
     * @param value The graphics
     */
    public void setGraphics(GraphicCollection3D value) {
        this.graphics = value;
    }

    /**
     * Get the number of graphics
     * @return The number of graphics
     */
    public int getGraphicNumber() {
        return this.graphics.size();
    }

    /**
     * Get if do screenshot
     * @return Boolean
     */
    public boolean isDoScreenShot() {
        return this.doScreenShot;
    }

    /**
     * Set if do screenshot
     * @param value Boolean
     */
    public void setDoScreenShot(boolean value) {
        this.doScreenShot = value;
    }

    /**
     * Get screen image
     *
     * @return Screen image
     */
    public BufferedImage getScreenImage() {
        return this.screenImage;
    }

    /**
     * Get extent of all graphics
     * @return Extent of all graphics
     */
    public Extent3D getGraphicExtent() {
        return this.graphicExtent;
    }

    @Override
    public Extent getExtent() {
        return this.graphicExtent;
    }

    /**
     * Get extent
     *
     * @return Extent
     */
    public Extent3D getDrawExtent() {
        return this.drawExtent;
    }

    /**
     * Set draw extent
     *
     * @param value Extent
     */
    @Override
    public void setDrawExtent(Extent extent) {
        this.drawExtent = (Extent3D) extent;
        this.transform.setExtent(this.drawExtent);

        if (!this.axesZoom) {
            setAxesExtent((Extent3D) drawExtent.clone());
        }
    }

    /**
     * Get axes extent (axes boundary extent)
     * @return Axes extent
     */
    public Extent3D getAxesExtent() {
        return this.axesExtent;
    }

    /**
     * Set axes extent
     * @param value Axes extent
     */
    public void setAxesExtent(Extent3D value) {
        this.axesExtent = value;
        xAxis.setMinMaxValue(axesExtent.minX, axesExtent.maxX);
        yAxis.setMinMaxValue(axesExtent.minY, axesExtent.maxY);
        zAxis.setMinMaxValue(axesExtent.minZ, axesExtent.maxZ);
    }

    /**
     * Get whether fix extent when add graphic
     * @return Whether fix extent
     */
    public boolean isFixExtent() {
        return this.fixExtent;
    }

    /**
     * Set whether fix extent when add graphic
     * @param value Whether fix extent
     */
    public void setFixExtent(boolean value) {
        this.fixExtent = value;
    }

    /**
     * Get width
     * @return Width
     */
    public int getWidth() {
        return this.width;
    }

    /**
     * Get height
     * @return Height
     */
    public int getHeight() {
        return this.height;
    }

    /**
     * Get if clip plane
     * @return Boolean
     */
    public boolean isClipPlane() {
        return this.clipPlane;
    }

    /**
     * Set if clip plane
     * @param value Boolean
     */
    public void setClipPlane(boolean value) {
        this.clipPlane = value;
    }

    /**
     * Get whether zooming axes (boundary - axis, base, bounding box, grid)
     * @return Whether zooming axes
     */
    public boolean isAxesZoom() {
        return this.axesZoom;
    }

    /**
     * Set whether zooming axes (boundary - axis, base, bounding box, grid)
     * @param value Whether zooming axes
     */
    public void setAxesZoom(boolean value) {
        this.axesZoom = value;
        if (this.axesZoom) {
            this.clipPlane = false;
        }
    }

    /**
     * Get aspect ratio
     * @return Aspect ratio
     */
    public float getAspectRatio() {
        return (float) this.width / this.height;
    }

    /**
     * Get box color
     *
     * @return Box color
     */
    public Color getBoxColor() {
        return this.boxColor;
    }

    /**
     * Set box color
     *
     * @param value Box color
     */
    public void setBoxColor(Color value) {
        this.boxColor = value;
    }

    /**
     * Get box line color
     *
     * @return Box line color
     *//*
    public Color getLineBoxColor() {
        return this.lineBoxColor;
    }

    *//**
     * Set box line color
     *
     * @param value Box line color
     *//*
    public void setLineBoxColor(Color value) {
        this.lineBoxColor = value;
    }*/

    /**
     * Get if draw base rectangle
     * @return Boolean
     */
    public boolean isDrawBase() {
        return this.drawBase;
    }

    /**
     * Set if draw base rectangle
     * @param value Boolean
     */
    public void setDrawBase(boolean value) {
        this.drawBase = value;
    }

    /**
     * Get if draw bounding box or not
     *
     * @return Boolean
     */
    public boolean isDrawBoundingBox() {
        return this.drawBoundingBox;
    }

    /**
     * Set if draw bounding box or not
     *
     * @param value Boolean
     */
    public void setDrawBoundingBox(boolean value) {
        this.drawBoundingBox = value;
    }

    /**
     * Set display X/Y axis or not
     *
     * @param value Boolean
     */
    public void setDisplayXY(boolean value) {
        this.displayXY = value;
    }

    /**
     * Set display Z axis or not
     *
     * @param value Boolean
     */
    public void setDisplayZ(boolean value) {
        this.displayZ = value;
    }

    /**
     * Get grid line
     *
     * @return Grid line
     */
    public GridLine getGridLine() {
        return this.gridLine;
    }

//    /**
//     * Get display grids or not
//     * @return Boolean
//     */
//    public boolean isDisplayGrids() {
//        return this.displayGrids;
//    }
//
//    /**
//     * Set display grids or not
//     *
//     * @param value Boolean
//     */
//    public void setDisplayGrids(boolean value) {
//        this.displayGrids = value;
//    }

    /**
     * Set display box or not
     *
     * @param value Boolean
     */
    public void setBoxed(boolean value) {
        this.boxed = value;
    }

    /**
     * Get x rotate angle
     *
     * @return X rotate angle
     */
    public float getAngleX() {
        return this.angleX;
    }

    /**
     * Set x rotate angle
     *
     * @param value X rotate angle
     */
    public void setAngleX(float value) {
        this.angleX = value;
    }

    /**
     * Get y rotate angle
     *
     * @return Y rotate angle
     */
    public float getAngleY() {
        return this.angleY;
    }

    /**
     * Set y rotate angle
     *
     * @param value Y rotate angle
     */
    public void setAngleY(float value) {
        this.angleY = value;
    }

    /**
     * Get head angle
     * @return Head angle
     */
    public float getHeadAngle() {
        return this.headAngle;
    }

    /**
     * Set head angle
     * @param value Head angle
     */
    public void setHeadAngle(float value) {
        this.headAngle = value;
    }

    /**
     * Get pitch angle
     * @return Pitch angle
     */
    public float getPitchAngle() {
        return this.pitchAngle;
    }

    /**
     * Set pitch angle
     * @param value Pitch angle
     */
    public void setPitchAngle(float value) {
        this.pitchAngle = value;
    }

    /**
     * Get title
     *
     * @return Title
     */
    public ChartText getTitle() {
        return this.title;
    }

    /**
     * Set title
     *
     * @param value Title
     */
    public void setTitle(ChartText value) {
        this.title = value;
    }

    /**
     * Set title
     *
     * @param text Title text
     */
    public void setTitle(String text) {
        if (this.title == null) {
            this.title = new ChartText(text);
        } else {
            this.title.setText(text);
        }
    }

    /**
     * Get legends
     *
     * @return Legends
     */
    public java.util.List<ChartLegend> getLegends() {
        return this.legends;
    }

    /**
     * Get chart legend
     *
     * @param idx Index
     * @return Chart legend
     */
    public ChartLegend getLegend(int idx) {
        if (this.legends.isEmpty()) {
            return null;
        } else {
            return this.legends.get(idx);
        }
    }

    /**
     * Get chart legend
     *
     * @return Chart legend
     */
    public ChartLegend getLegend() {
        if (this.legends.isEmpty()) {
            return null;
        } else {
            return this.legends.get(this.legends.size() - 1);
        }
    }

    /**
     * Set chart legend
     *
     * @param value Legend
     */
    public void setLegend(ChartLegend value) {
        this.legends.clear();
        this.legends.add(value);
    }

    /**
     * Set legends
     *
     * @param value Legends
     */
    public void setLegends(java.util.List<ChartLegend> value) {
        this.legends = value;
    }

    /**
     * Get x axis
     *
     * @return X axis
     */
    public Axis getXAxis() {
        return this.xAxis;
    }

    /**
     * Get y axis
     *
     * @return Y axis
     */
    public Axis getYAxis() {
        return this.yAxis;
    }

    /**
     * Get z axis
     *
     * @return Z axis
     */
    public Axis getZAxis() {
        return this.zAxis;
    }

    /**
     * Get aspect type
     * @return Aspect type
     */
    public AspectType getAspectType() {
        return this.aspectType;
    }

    /**
     * Set aspect type
     * @param value Aspect type
     */
    public void setAspectType(AspectType value) {
        this.aspectType = value;
        this.transform.setAspectType(this.aspectType);
    }

    /**
     * Get z scale
     * @return Z scale
     */
    public float getZScale() {
        return this.transform.getZScale();
    }

    /**
     * Set z scale
     * @param value Z scale
     */
    public void setZScale(float value) {
        this.transform.zScale = value;
    }

    /**
     * Get x minimum
     *
     * @return X minimum
     */
    public float getXMin() {
        return (float) this.drawExtent.minX;
    }

    /**
     * Set minimum x
     *
     * @param value Minimum x
     */
    public void setXMin(float value) {
        this.drawExtent.minX = value;
        updateExtent();
        this.xAxis.setMinMaxValue(drawExtent.minX, drawExtent.maxX);
    }

    /**
     * Get x maximum
     *
     * @return X maximum
     */
    public float getXMax() {
        return (float) this.drawExtent.maxX;
    }

    /**
     * Set maximum x
     *
     * @param value Maximum x
     */
    public void setXMax(float value) {
        this.drawExtent.maxX = value;
        updateExtent();
        this.xAxis.setMinMaxValue(drawExtent.minX, drawExtent.maxX);
    }

    /**
     * Set x minimum and maximum values
     *
     * @param min Minimum value
     * @param max Maximum value
     */
    public void setXMinMax(float min, float max) {
        this.drawExtent.minX = min;
        this.drawExtent.maxX = max;
        updateExtent();
        this.xAxis.setMinMaxValue(min, max);
        this.fixExtent = true;
    }

    /**
     * Get y minimum
     *
     * @return Y minimum
     */
    public float getYMin() {
        return (float) this.drawExtent.minY;
    }

    /**
     * Set minimum y
     *
     * @param value Minimum y
     */
    public void setYMin(float value) {
        this.drawExtent.minY = value;
        updateExtent();
        this.yAxis.setMinMaxValue(drawExtent.minY, drawExtent.maxY);
    }

    /**
     * Get y maximum
     *
     * @return Y maximum
     */
    public float getYMax() {
        return (float) this.drawExtent.maxY;
    }

    /**
     * Set Maximum y
     *
     * @param value Maximum y
     */
    public void setYMax(float value) {
        this.drawExtent.maxY = value;
        updateExtent();
        this.yAxis.setMinMaxValue(drawExtent.minY, drawExtent.maxY);
    }

    /**
     * Set y minimum and maximum values
     *
     * @param min Minimum value
     * @param max Maximum value
     */
    public void setYMinMax(float min, float max) {
        this.drawExtent.minY = min;
        this.drawExtent.maxY = max;
        updateExtent();
        this.yAxis.setMinMaxValue(min, max);
        this.fixExtent = true;
    }

    /**
     * Get z minimum
     *
     * @return Z minimum
     */
    public float getZMin() {
        return (float) this.drawExtent.minZ;
    }

    /**
     * Set minimum z
     *
     * @param value Minimum z
     */
    public void setZMin(float value) {
        this.drawExtent.minZ = value;
        updateExtent();
        this.zAxis.setMinMaxValue(drawExtent.minZ, drawExtent.maxZ);
    }

    /**
     * Get z maximum
     *
     * @return Z maximum
     */
    public float getZMax() {
        return (float) this.drawExtent.maxZ;
    }

    /**
     * Set maximum z
     *
     * @param value Maximum z
     */
    public void setZMax(float value) {
        this.drawExtent.maxZ = value;
        updateExtent();
        this.zAxis.setMinMaxValue(drawExtent.minZ, drawExtent.maxZ);
    }

    /**
     * Set z minimum and maximum values
     *
     * @param min Minimum value
     * @param max Maximum value
     */
    public void setZMinMax(float min, float max) {
        this.drawExtent.minZ = min;
        this.drawExtent.maxZ = max;
        updateExtent();
        this.zAxis.setMinMaxValue(min, max);
        this.fixExtent = true;
    }

    /**
     * Get lighting set
     *
     * @return Lighting set
     */
    public Lighting getLighting() {
        return this.lighting;
    }

    /**
     * Set lighting set
     *
     * @param value Lighting set
     */
    public void setLighting(Lighting value) {
        this.lighting = value;
    }

    /**
     * Get is antialias or not
     * @return Antialias or not
     */
    public boolean isAntialias() { return this.antialias; }

    /**
     * Set is antialias or not
     * @param value Antialias or not
     */
    public void setAntialias(boolean value) { this.antialias = value; }

    /**
     * Get DPI scale
     * @return DPI scale
     */
    public float getDpiScale() {
        return this.dpiScale;
    }

    /**
     * Set DPI scale
     * @param value DPI scale
     */
    public void setDpiScale(float value) {
        this.dpiScale = value;
    }

    /**
     * Get is always update buffers or not
     * @return Is always update buffers or not
     */
    public boolean isAlwaysUpdateBuffers() {
        return this.alwaysUpdateBuffers;
    }

    /**
     * Set whether always update buffers
     * @param value Whether always update buffers
     */
    public void setAlwaysUpdateBuffers(boolean value) {
        this.alwaysUpdateBuffers = value;
    }

    /**
     * Get is orthographic or not
     * @return is orthographic or not
     */
    public boolean isOrthographic() {
        return this.orthographic;
    }

    /**
     * Set orthographic
     * @param value Orthographic or not
     */
    public void setOrthographic(boolean value) {
        this.orthographic = value;

        if (this.drawable != null) {
            this.drawable.invoke(true, new GLRunnable() {
                public boolean run(GLAutoDrawable drawable) {
                    updateProjections(drawable);
                    return false;
                }
            });
        }
    }

    /**
     * Get camera distance
     * @return Camera distance
     */
    public float getDistance() {
        return this.distance;
    }

    /**
     * Set camera distance
     * @param value Camera distance
     */
    public void setDistance(float value) {
        this.distance = value;
    }

    /**
     * Get field of view angle for perspective projection
     * @return Field of view angle in degrees
     */
    public float getFieldOfView() {
        return this.fieldOfView;
    }

    /**
     * Set field of view angle for perspective projection
     * @param value Field of view angle in degrees
     */
    public void setFieldOfView(float value) {
        this.fieldOfView = value;
    }

    // </editor-fold>
    // <editor-fold desc="methods">
    /**
     * Add a legend
     *
     * @param legend The legend
     */
    public void addLegend(ChartLegend legend) {
        this.legends.clear();
        this.legends.add(legend);
    }

    /**
     * Remove a legend
     *
     * @param legend The legend
     */
    public void removeLegend(ChartLegend legend) {
        this.legends.remove(legend);
    }

    /**
     * Remove a legend by index
     *
     * @param idx The legend index
     */
    public void removeLegend(int idx) {
        this.legends.remove(idx);
    }

    /**
     * Get outer position area
     *
     * @param area Whole area
     * @return Position area
     */
    @Override
    public Rectangle2D getOuterPositionArea(Rectangle2D area) {
        Rectangle2D rect = this.getOuterPosition();
        double x = area.getWidth() * rect.getX() + area.getX();
        double y = area.getHeight() * (1 - rect.getHeight() - rect.getY()) + area.getY();
        double w = area.getWidth() * rect.getWidth();
        double h = area.getHeight() * rect.getHeight();
        return new Rectangle2D.Double(x, y, w, h);
    }

    @Override
    public Dataset getDataset() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void setDataset(Dataset dataset) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public PlotType getPlotType() {
        return PlotType.XYZ;
    }

    @Override
    public void draw(Graphics2D g2, Rectangle2D area) {

    }

    protected void updateExtent() {
        this.transform.setExtent(this.drawExtent);
        this.setAxesExtent((Extent3D) this.drawExtent.clone());
    }

    /**
     * Set axis tick font
     *
     * @param font Font
     */
    public void setAxisTickFont(Font font) {
        this.xAxis.setTickLabelFont(font);
        this.yAxis.setTickLabelFont(font);
        this.zAxis.setTickLabelFont(font);
    }

    /**
     * Add a graphic
     *
     * @param graphic Graphic
     */
    public void addGraphic(Graphic graphic) {
        this.graphics.add(graphic);
        Extent ex = this.graphics.getExtent();
        if (!ex.is3D()) {
            ex = ex.to3D();
        }
        ex.asNonZero();
        this.graphicExtent = (Extent3D) ex;
        if (!fixExtent) {
            this.setAxesExtent((Extent3D) graphicExtent.clone());
            this.setDrawExtent((Extent3D) this.graphicExtent.clone());
        }
    }

    /**
     * Add a graphic
     *
     * @param index Index
     * @param graphic Graphic
     */
    public void addGraphic(int index, Graphic graphic) {
        this.graphics.add(index, graphic);
        Extent ex = this.graphics.getExtent();
        if (!ex.is3D()) {
            ex = ex.to3D();
        }
        ex.asNonZero();
        this.graphicExtent = (Extent3D) ex;
        if (!fixExtent) {
            this.setAxesExtent((Extent3D) graphicExtent.clone());
            this.setDrawExtent((Extent3D) this.graphicExtent.clone());
        }
    }

    /**
     * Add a graphic
     *
     * @param graphic The graphic
     * @param proj The graphic projection
     */
    public void addGraphic(Graphic graphic, ProjectionInfo proj) {
        if (this.projInfo == null || proj.equals(this.projInfo)) {
            addGraphic(graphic);
        } else {
            Graphic nGraphic = ProjectionUtil.projectGraphic(graphic, proj, this.projInfo);
            addGraphic(nGraphic);
        }
    }

    /**
     * Add a graphic
     *
     * @param index The index
     * @param graphic The graphic
     * @param proj The graphic projection
     */
    public void addGraphic(int index, Graphic graphic, ProjectionInfo proj) {
        if (this.projInfo == null || proj.equals(this.projInfo)) {
            addGraphic(index, graphic);
        } else {
            Graphic nGraphic = ProjectionUtil.projectGraphic(graphic, proj, this.projInfo);
            addGraphic(index, nGraphic);
        }
    }

    /**
     * Remove a graphic
     *
     * @param graphic The graphic to be removed
     */
    public void removeGraphic(Graphic graphic) {
        if (this.graphics.contains(graphic)) {
            this.graphics.remove(graphic);
            if (renderMap.containsKey(graphic)) {
                renderMap.remove(graphic);
            }
        }
    }

    /**
     * Remove a graphic by index
     *
     * @param idx Index
     */
    public void removeGraphic(int idx) {
        this.graphics.remove(this.graphics.get(idx));
    }

    /**
     * Remove last graphic
     */
    public void removeLastGraphic() {
        this.graphics.remove(this.graphics.size() - 1);
    }

    /**
     * Remove all graphics
     */
    public void removeAllGraphics() {
        this.graphics.clear();
        this.renderMap.clear();
    }

    /**
     * Set auto extent
     */
    public void setAutoExtent() {
    }

    /**
     * Add z axis locations
     * @param x Location x
     * @param y Location y
     * @param left Whether left tick
     */
    public void addZAxis(float x, float y, boolean left) {
        this.zAxisLocations.add(new ZAxisOption(x, y, left));
    }

    public void display(GLAutoDrawable drawable) {
        final GL2 gl = drawable.getGL().getGL2();
        gl.glLoadIdentity();

        this.updateTextRender(this.xAxis.getTickLabelFont());

        //Set light position - follow glLoadIdentity
        this.lighting.setPosition(gl);

        gl.glPushMatrix();

        this.modelViewMatrix = new Matrix4f();
        modelViewMatrix.rotate((float) Math.toRadians(angleX), 1.0f, 0.0f, 0.0f);
        modelViewMatrix.rotate((float) Math.toRadians(angleY), 0.0f, 0.0f, 1.0f);
        if (headAngle != 0) {
            modelViewMatrix.rotate((float) Math.toRadians(headAngle), 0.0f, 1.0f, 0.0f);
        }
        modelViewMatrixR = new Matrix4f(modelViewMatrix);

        Vector3f center = transform.getCenter();
        Vector3f scale = transform.getScale();
        modelViewMatrix.scale(scale);
        modelViewMatrix.translate(center.negate());

        FloatBuffer fb = Buffers.newDirectFloatBuffer(16);
        gl.glLoadMatrixf(modelViewMatrix.get(fb));

        this.updateMatrix(gl);

        //Draw base
        if (this.drawBase) {
            this.drawBase(gl);
        }

        //Draw box
        if (this.boxed) {
            this.drawBox(gl);
        }

        //Draw graphics
        if (this.clipPlane) {
            enableClipPlane(gl);
        }

        //Draw grid line
        drawGridLine(gl);

        //Lighting
        this.setLight(gl);

        for (int m = 0; m < this.graphics.getNumGraphics(); m++) {
            Graphic graphic = this.graphics.get(m);
            drawGraphics(gl, graphic);
        }

        if (this.clipPlane) {
            disableClipPlane(gl);
        }

        //Stop lighting
        if (this.lighting.isEnable()) {
            this.lighting.stop(gl);
        }

        //Draw bounding box
        if (this.drawBoundingBox) {
            this.drawBoundingBox(gl);
        }

        //Draw axis
        this.drawAxis(gl);
        this.drawAllZAxis(gl);

        //Draw legend
        gl.glPopMatrix();
        this.updateMatrix(gl);
        if (!this.legends.isEmpty()) {
            ChartColorBar legend = (ChartColorBar) this.legends.get(0);
            updateTextRender(legend.getTickLabelFont());
            if (legend.getLegendScheme().getColorMap() == null)
                this.drawLegend(gl, legend);
            else
                this.drawColorbar(gl, legend);
        }

        //Draw title
        this.drawTitle();

        this.textRenderer.dispose();
        this.textRenderer = null;

        gl.glFlush();

        /*//Do screenshot
        if (this.doScreenShot) {
            AWTGLReadBufferUtil glReadBufferUtil = new AWTGLReadBufferUtil(drawable.getGLProfile(), false);
            this.screenImage = glReadBufferUtil.readPixelsToBufferedImage(drawable.getGL(), true);
            this.doScreenShot = false;
        }*/

        //Disable always update buffers
        if (this.alwaysUpdateBuffers)
            this.alwaysUpdateBuffers = false;
    }

    private void disableClipPlane(GL2 gl) {
        gl.glDisable(GL2.GL_CLIP_PLANE0);
        gl.glDisable(GL2.GL_CLIP_PLANE1);
        gl.glDisable(GL2.GL_CLIP_PLANE2);
        gl.glDisable(GL2.GL_CLIP_PLANE3);
        gl.glDisable(GL2.GL_CLIP_PLANE4);
        gl.glDisable(GL2.GL_CLIP_PLANE5);
    }

    private void enableClipPlane(GL2 gl) {
        float xMin = (float) axesExtent.minX - (float) axesExtent.getWidth() * 0.001f;
        float xMax = (float) axesExtent.maxX + (float) axesExtent.getWidth() * 0.001f;
        float yMin = (float) axesExtent.minY - (float) axesExtent.getHeight() * 0.001f;
        float yMax = (float) axesExtent.maxY + (float) axesExtent.getHeight() * 0.001f;
        float zMin = (float) axesExtent.minZ - (float) axesExtent.getZLength() * 0.001f;
        float zMax = (float) axesExtent.maxZ + (float) axesExtent.getZLength() * 0.001f;

        gl.glClipPlane(GL2.GL_CLIP_PLANE0, new double[]{-1, 0, 0, xMax}, 0);
        gl.glEnable(GL2.GL_CLIP_PLANE0);
        gl.glClipPlane(GL2.GL_CLIP_PLANE1, new double[]{1, 0, 0, -xMin}, 0);
        gl.glEnable(GL2.GL_CLIP_PLANE1);
        gl.glClipPlane(GL2.GL_CLIP_PLANE2, new double[]{0, -1, 0, yMax}, 0);
        gl.glEnable(GL2.GL_CLIP_PLANE2);
        gl.glClipPlane(GL2.GL_CLIP_PLANE3, new double[]{0, 1, 0, -yMin}, 0);
        gl.glEnable(GL2.GL_CLIP_PLANE3);
        gl.glClipPlane(GL2.GL_CLIP_PLANE4, new double[]{0, 0, -1, zMax}, 0);
        gl.glEnable(GL2.GL_CLIP_PLANE4);
        gl.glClipPlane(GL2.GL_CLIP_PLANE5, new double[]{0, 0, 1, -zMin}, 0);
        gl.glEnable(GL2.GL_CLIP_PLANE5);
    }

    protected void setLight(GL2 gl) {
        //Set lighting
        if (this.lighting.isEnable()) {

            this.lighting.start(gl);
            //keep material colors
            //gl.glColorMaterial(GL_FRONT_AND_BACK, GL2.GL_DIFFUSE);
            gl.glColorMaterial(GL2.GL_FRONT_AND_BACK, GL2.GL_AMBIENT_AND_DIFFUSE);
            gl.glEnable(GL2.GL_COLOR_MATERIAL);
            //double side normalize
            gl.glLightModeli(GL2.GL_LIGHT_MODEL_TWO_SIDE, GL2.GL_TRUE);
            //gl.glEnable(GL2.GL_RESCALE_NORMAL);
            gl.glEnable(GL2.GL_NORMALIZE);
            //gl.glMaterialfv(GL2.GL_FRONT_AND_BACK, GL2.GL_DIFFUSE, FloatBuffer.wrap(this.lighting.mat_diffuse));
        }
    }

    /**
     * @param gl The GL context.
     * @param glu The GL unit.
     * @param distance The distance from the screen.
     */
    private void setCamera(GL2 gl, GLU glu, float distance) {
        // Change to projection matrix.
        gl.glMatrixMode(GL2.GL_PROJECTION);
        gl.glLoadIdentity();

        // Perspective.
        float widthHeightRatio = (float) this.viewport[2] / (float) this.viewport[3];
        glu.gluPerspective(45, widthHeightRatio, 1, 1000);
        glu.gluLookAt(0, 0, distance, 0, 0, 0, 0, 1, 0);
        //glu.gluLookAt(0, 0, 0, 0, 0, -1, 0, 1, 0);

        // Change back to model view matrix.
        gl.glMatrixMode(GL2.GL_MODELVIEW);
        gl.glLoadIdentity();
    }

    /**
     * Draws the base plane. The base plane is the x-y plane.
     *
     * @param g the graphics context to draw.
     * @param x used to retrieve x coordinates of drawn plane from this method.
     * @param y used to retrieve y coordinates of drawn plane from this method.
     */
    protected void drawBase(GL2 gl) {
        float xMin, xMax, yMin, yMax, zMin;
        xMin = (float) axesExtent.minX;
        xMax = (float) axesExtent.maxX;
        yMin = (float) axesExtent.minY;
        yMax = (float) axesExtent.maxY;
        zMin = (float) axesExtent.minZ;
        /*xMin = this.transform.transform_x((float) axesExtent.minX);
        xMax = this.transform.transform_x((float) axesExtent.maxX);
        yMin = this.transform.transform_y((float) axesExtent.minY);
        yMax = this.transform.transform_y((float) axesExtent.maxY);
        zMin = this.transform.transform_z((float) axesExtent.minZ);*/

        float[] rgba = this.gridLine.getColor().getRGBComponents(null);
        gl.glColor4f(rgba[0], rgba[1], rgba[2], rgba[3]);
        gl.glLineWidth(this.gridLine.getSize() * this.dpiScale);
        gl.glBegin(GL2.GL_LINE_STRIP);
        gl.glVertex3f(xMin, yMax, zMin);
        gl.glVertex3f(xMin, yMin, zMin);
        gl.glVertex3f(xMax, yMin, zMin);
        gl.glVertex3f(xMax, yMax, zMin);
        gl.glVertex3f(xMin, yMax, zMin);
        gl.glEnd();
    }

    protected Matrix4f toMatrix(float[] data) {
        Matrix4f matrix4f = new Matrix4f();
        matrix4f.set(data);
        return matrix4f;
    }

    protected void updateMatrix(GL2 gl) {
        gl.glGetIntegerv(GL2.GL_VIEWPORT, viewport, 0);
        gl.glGetFloatv(GL2.GL_MODELVIEW_MATRIX, mvmatrix, 0);
        gl.glGetFloatv(GL2.GL_PROJECTION_MATRIX, projmatrix, 0);
        modelViewMatrix = toMatrix(mvmatrix);
        projectionMatrix = toMatrix(projmatrix);
        viewProjMatrix = projectionMatrix.
                mul(modelViewMatrix);
    }

    /**
     * Get 3D coordinates from screen 2D coordinates
     * @param x X coordinate
     * @param y Y coordinate
     * @return 3D coordinates
     */
    public Vector3f unProject(float x, float y) {
        if (this.gl == null) {
            return new Vector3f();
        }

        y = viewport[3] - y;
        FloatBuffer buffer = FloatBuffer.allocate(4);
        gl.glReadPixels((int)x, (int)y, 1, 1, gl.GL_DEPTH_COMPONENT, gl.GL_FLOAT, buffer);
        float z = buffer.get();
        float[] out = new float[4];
        glu.gluUnProject(
                x,
                y,
                z,
                mvmatrix, 0,
                projmatrix, 0,
                viewport, 0,
                out, 0
        );

        return new Vector3f(out[0], out[1], out[2]);
    }

    /**
     * Get 3D coordinates from screen 2D coordinates
     * @param x X coordinate
     * @param y Y coordinate
     * @param gl GL2
     * @return 3D coordinates
     */
    public Vector3f unProject(float x, float y, GL2 gl) {
        y = viewport[3] - y;
        FloatBuffer buffer = FloatBuffer.allocate(4);
        gl.glReadPixels((int)x, (int)y, 1, 1, gl.GL_DEPTH_COMPONENT, gl.GL_FLOAT, buffer);
        float z = buffer.get();
        float[] out = new float[4];
        glu.gluUnProject(
                x,
                y,
                z,
                mvmatrix, 0,
                projmatrix, 0,
                viewport, 0,
                out, 0
        );

        return new Vector3f(out[0], out[1], out[2]);
    }

    protected Vector2f toScreen(float vx, float vy, float vz) {
        //Get screen coordinates
        float coord[] = new float[4];// x, y;// returned xy 2d coords
        glu.gluProject(vx, vy, vz, mvmatrix, 0, projmatrix, 0, viewport, 0, coord, 0);
        if (viewport[0] != 0)
            coord[0] -= viewport[0];
        if (viewport[1] != 0)
            coord[1] -= viewport[1];

        return new Vector2f(coord[0], coord[1]);
    }

    protected float toScreenLength(float x1, float y1, float z1, float x2, float y2, float z2) {
        Vector2f coord = toScreen(x1, y1, z1);
        float sx1 = coord.x;
        float sy1 = coord.y;
        coord = toScreen(x2, y2, z2);
        float sx2 = coord.x;
        float sy2 = coord.y;

        return (float) Math.sqrt(Math.pow(sx2 - sx1, 2) + Math.pow(sy2 - sy1, 2));
    }

    protected float toScreenAngle(float x1, float y1, float z1, float x2, float y2, float z2) {
        Vector2f coord = toScreen(x1, y1, z1);
        float sx1 = coord.x;
        float sy1 = coord.y;
        coord = toScreen(x2, y2, z2);
        float sx2 = coord.x;
        float sy2 = coord.y;

        return (float) MeteoMath.uv2ds(sx2 - sx1, sy2 - sy1)[0];
    }

    protected int getLabelGap(Font font, java.util.List<ChartText> labels, double len) {
        TextRenderer textRenderer = new TextRenderer(font);
        int n = labels.size();
        int nn;
        Rectangle2D rect = textRenderer.getBounds("Text".subSequence(0, 4));
        nn = (int) (len / rect.getHeight());
        if (nn == 0) {
            nn = 1;
        }
        return n / nn + 1;
    }

    protected int getLegendTickGap(ChartColorBar legend, double len) {
        if (legend.getTickLabelAngle() != 0) {
            return 1;
        }

        Font font = legend.getTickLabelFont();
        if (this.dpiScale != 1) {
            font = new Font(font.getFontName(), font.getStyle(), (int)(font.getSize() * this.dpiScale));
        }
        TextRenderer textRenderer = new TextRenderer(font);
        int n = legend.getLegendScheme().getBreakNum();
        int nn;
        Rectangle2D rect = textRenderer.getBounds("Text".subSequence(0, 4));
        nn = (int) (len / rect.getHeight());
        if (nn == 0) {
            nn = 1;
        }
        return n / nn + 1;
    }

    protected void drawBox(GL2 gl) {
        float xMin, xMax, yMin, yMax, zMin, zMax;
        xMin = (float) axesExtent.minX;
        xMax = (float) axesExtent.maxX;
        yMin = (float) axesExtent.minY;
        yMax = (float) axesExtent.maxY;
        zMin = (float) axesExtent.minZ;
        zMax = (float) axesExtent.maxZ;

        float[] rgba = this.gridLine.getColor().getRGBComponents(null);
        gl.glColor4f(rgba[0], rgba[1], rgba[2], rgba[3]);
        gl.glLineWidth(this.gridLine.getSize() * this.dpiScale);
        if (this.angleY >= 180 && this.angleY < 360) {
            gl.glBegin(GL2.GL_LINE_STRIP);
            gl.glVertex3f(xMin, yMax, zMin);
            gl.glVertex3f(xMin, yMin, zMin);
            gl.glVertex3f(xMin, yMin, zMax);
            gl.glVertex3f(xMin, yMax, zMax);
            gl.glVertex3f(xMin, yMax, zMin);
            gl.glEnd();
        } else {
            gl.glBegin(GL2.GL_LINE_STRIP);
            gl.glVertex3f(xMax, yMax, zMin);
            gl.glVertex3f(xMax, yMin, zMin);
            gl.glVertex3f(xMax, yMin, zMax);
            gl.glVertex3f(xMax, yMax, zMax);
            gl.glVertex3f(xMax, yMax, zMin);
            gl.glEnd();
        }
        if (this.angleY >= 90 && this.angleY < 270) {
            gl.glBegin(GL2.GL_LINE_STRIP);
            gl.glVertex3f(xMin, yMin, zMin);
            gl.glVertex3f(xMax, yMin, zMin);
            gl.glVertex3f(xMax, yMin, zMax);
            gl.glVertex3f(xMin, yMin, zMax);
            gl.glVertex3f(xMin, yMin, zMin);
            gl.glEnd();
        } else {
            gl.glBegin(GL2.GL_LINE_STRIP);
            gl.glVertex3f(xMin, yMax, zMin);
            gl.glVertex3f(xMax, yMax, zMin);
            gl.glVertex3f(xMax, yMax, zMax);
            gl.glVertex3f(xMin, yMax, zMax);
            gl.glVertex3f(xMin, yMax, zMin);
            gl.glEnd();
        }
    }

    protected void drawBoundingBox(GL2 gl) {
        float xMin, xMax, yMin, yMax, zMin, zMax;
        xMin = (float) axesExtent.minX;
        xMax = (float) axesExtent.maxX;
        yMin = (float) axesExtent.minY;
        yMax = (float) axesExtent.maxY;
        zMin = (float) axesExtent.minZ;
        zMax = (float) axesExtent.maxZ;

        float[] rgba = this.gridLine.getColor().getRGBComponents(null);
        gl.glColor4f(rgba[0], rgba[1], rgba[2], rgba[3]);
        gl.glLineWidth(this.gridLine.getSize() * this.dpiScale);
        if (this.angleY >= 180 && this.angleY < 360) {
            gl.glBegin(GL2.GL_LINE_STRIP);
            gl.glVertex3f(xMax, yMax, zMin);
            gl.glVertex3f(xMax, yMin, zMin);
            gl.glVertex3f(xMax, yMin, zMax);
            gl.glVertex3f(xMax, yMax, zMax);
            gl.glVertex3f(xMax, yMax, zMin);
            gl.glEnd();
        } else {
            gl.glBegin(GL2.GL_LINE_STRIP);
            gl.glVertex3f(xMin, yMax, zMin);
            gl.glVertex3f(xMin, yMin, zMin);
            gl.glVertex3f(xMin, yMin, zMax);
            gl.glVertex3f(xMin, yMax, zMax);
            gl.glVertex3f(xMin, yMax, zMin);
            gl.glEnd();
        }
        if (this.angleY >= 90 && this.angleY < 270) {
            gl.glBegin(GL2.GL_LINE_STRIP);
            gl.glVertex3f(xMin, yMax, zMin);
            gl.glVertex3f(xMax, yMax, zMin);
            gl.glVertex3f(xMax, yMax, zMax);
            gl.glVertex3f(xMin, yMax, zMax);
            gl.glVertex3f(xMin, yMax, zMin);
            gl.glEnd();
        } else {
            gl.glBegin(GL2.GL_LINE_STRIP);
            gl.glVertex3f(xMin, yMin, zMin);
            gl.glVertex3f(xMax, yMin, zMin);
            gl.glVertex3f(xMax, yMin, zMax);
            gl.glVertex3f(xMin, yMin, zMax);
            gl.glVertex3f(xMin, yMin, zMin);
            gl.glEnd();
        }
    }

    protected void drawXYGridLine(GL2 gl) {
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

        float[] rgba;
        float x, y, x1, y1, v;
        int skip;
        XAlign xAlign;
        YAlign yAlign;
        Rectangle2D rect;
        float strWidth, strHeight;
        if (this.displayXY) {
            if (this.angleY >= 90 && this.angleY < 270) {
                y = yMax;
                y1 = yMin;
            } else {
                y = yMin;
                y1 = yMax;
            }

            this.xAxis.updateTickLabels();
            java.util.List<ChartText> tlabs = this.xAxis.getTickLabels();
            float axisLen = this.toScreenLength(xMin, y, zMin, xMax, y, zMin);
            skip = getLabelGap(this.xAxis.getTickLabelFont(), tlabs, axisLen);
            for (int i = 0; i < this.xAxis.getTickValues().length; i += skip) {
                v = (float) this.xAxis.getTickValues()[i];
                if (v <= axesExtent.minX || v >= axesExtent.maxX) {
                    continue;
                }
                //v = this.transform.transform_x(v);
                if (i == tlabs.size()) {
                    break;
                }

                //Draw grid line
                if (this.gridLine.isDrawXLine()) {
                    rgba = this.gridLine.getColor().getRGBComponents(null);
                    gl.glColor4f(rgba[0], rgba[1], rgba[2], rgba[3]);
                    gl.glLineWidth(this.gridLine.getSize() * this.dpiScale);
                    gl.glBegin(GL2.GL_LINES);
                    gl.glVertex3f(v, y, zMin);
                    gl.glVertex3f(v, y1, zMin);
                    gl.glEnd();
                    if (this.displayZ && this.boxed) {
                        gl.glBegin(GL2.GL_LINES);
                        gl.glVertex3f(v, y1, zMin);
                        gl.glVertex3f(v, y1, zMax);
                        gl.glEnd();
                    }
                }
            }

            ////////////////////////////////////////////
            //y grid line
            if (this.angleY >= 180 && this.angleY < 360) {
                x = xMax;
                x1 = xMin;
            } else {
                x = xMin;
                x1 = xMax;
            }

            this.yAxis.updateTickLabels();
            tlabs = this.yAxis.getTickLabels();
            axisLen = this.toScreenLength(x, yMin, zMin, x, yMax, zMin);
            skip = getLabelGap(this.yAxis.getTickLabelFont(), tlabs, axisLen);
            for (int i = 0; i < this.yAxis.getTickValues().length; i += skip) {
                v = (float) this.yAxis.getTickValues()[i];
                if (v <= axesExtent.minY || v >= axesExtent.maxY) {
                    continue;
                }
                //v = this.transform.transform_y(v);
                if (i == tlabs.size()) {
                    break;
                }

                //Draw grid line
                if (this.gridLine.isDrawYLine()) {
                    rgba = this.gridLine.getColor().getRGBComponents(null);
                    gl.glColor4f(rgba[0], rgba[1], rgba[2], rgba[3]);
                    gl.glLineWidth(this.gridLine.getSize() * this.dpiScale);
                    gl.glBegin(GL2.GL_LINES);
                    gl.glVertex3f(x, v, zMin);
                    gl.glVertex3f(x1, v, zMin);
                    gl.glEnd();
                    if (this.displayZ && this.boxed) {
                        gl.glBegin(GL2.GL_LINES);
                        gl.glVertex3f(x1, v, zMin);
                        gl.glVertex3f(x1, v, zMax);
                        gl.glEnd();
                    }
                }
            }
        }
    }

    protected void drawZGridLine(GL2 gl) {
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

        float[] rgba;
        float x, y, x1, y1, v;
        int skip;
        XAlign xAlign;
        YAlign yAlign;
        Rectangle2D rect;
        float strWidth, strHeight;
        //z axis line
        if (this.angleY < 90) {
            x = xMin;
            x1 = xMax;
            y = yMax;
            y1 = yMin;
        } else if (this.angleY < 180) {
            x = xMax;
            x1 = xMin;
            y = yMax;
            y1 = yMin;
        } else if (this.angleY < 270) {
            x = xMax;
            x1 = xMin;
            y = yMin;
            y1 = yMax;
        } else {
            x = xMin;
            x1 = xMax;
            y = yMin;
            y1 = yMax;
        }

        this.zAxis.updateTickLabels();
        java.util.List<ChartText> tlabs = this.zAxis.getTickLabels();
        float axisLen = this.toScreenLength(x, y, zMin, x, y, zMax);
        skip = getLabelGap(this.zAxis.getTickLabelFont(), tlabs, axisLen);
        for (int i = 0; i < this.zAxis.getTickValues().length; i += skip) {
            v = (float) this.zAxis.getTickValues()[i];
            if (v <= axesExtent.minZ || v >= axesExtent.maxZ) {
                continue;
            }
            //v = this.transform.transform_z(v);
            if (i == tlabs.size()) {
                break;
            }

            //Draw grid line
            if (this.gridLine.isDrawZLine() && this.boxed) {
                rgba = this.gridLine.getColor().getRGBComponents(null);
                gl.glColor4f(rgba[0], rgba[1], rgba[2], rgba[3]);
                gl.glLineWidth(this.gridLine.getSize() * this.dpiScale);
                gl.glBegin(GL2.GL_LINE_STRIP);
                gl.glVertex3f(x, y, v);
                if (x < x1) {
                    if (y > y1) {
                        gl.glVertex3f(x1, y, v);
                        gl.glVertex3f(x1, y1, v);
                    } else {
                        gl.glVertex3f(x, y1, v);
                        gl.glVertex3f(x1, y1, v);
                    }
                } else {
                    if (y > y1) {
                        gl.glVertex3f(x, y1, v);
                        gl.glVertex3f(x1, y1, v);
                    } else {
                        gl.glVertex3f(x1, y, v);
                        gl.glVertex3f(x1, y1, v);
                    }
                }
                gl.glEnd();
            }
        }
    }

    protected void drawGridLine(GL2 gl) {
        //Draw x/y grid line
        if (this.displayXY) {
            this.drawXYGridLine(gl);
        }

        //Draw z grid line
        if (this.displayZ) {
            this.drawZGridLine(gl);
        }
    }

    protected void drawBoxGrids(GL2 gl) {
        //Draw base
        if (this.drawBase) {
            this.drawBase(gl);
        }

        //Draw box
        if (this.boxed) {
            this.drawBox(gl);
        }

        //Draw grid lines
        this.drawGridLine(gl);
    }

    protected void drawAxis(GL2 gl) {
        float xMin, xMax, yMin, yMax, zMin, zMax;
        xMin = (float) axesExtent.minX;
        xMax = (float) axesExtent.maxX;
        yMin = (float) axesExtent.minY;
        yMax = (float) axesExtent.maxY;
        zMin = (float) axesExtent.minZ;
        zMax = (float) axesExtent.maxZ;

        gl.glDepthFunc(GL.GL_ALWAYS);

        //Draw axis
        float[] rgba;
        float x, y, v;
        int skip;
        XAlign xAlign;
        YAlign yAlign;
        Rectangle2D rect;
        Vector3f center = this.transform.getCenter();
        float strWidth, strHeight;
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

            //x axis ticks
            float tickLen = this.xAxis.getTickLength() * this.lenScale * transform.getYLength() / 2;
            this.xAxis.updateTickLabels();
            java.util.List<ChartText> tlabs = this.xAxis.getTickLabels();
            if (tlabs.size() > 0) {
                float axisLen = this.toScreenLength(xMin, y, zMin, xMax, y, zMin);
                skip = getLabelGap(this.xAxis.getTickLabelFont(), tlabs, axisLen);
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
                if (this.xAxis.isDrawTickLabel()) {
                    this.updateTextRender(tlabs.get(0).getFont());
                }
                for (int i = 0; i < this.xAxis.getTickValues().length; i += skip) {
                    v = (float) this.xAxis.getTickValues()[i];
                    if (v < axesExtent.minX || v > axesExtent.maxX) {
                        continue;
                    }
                    //v = this.transform.transform_x(v);
                    if (i == tlabs.size()) {
                        break;
                    }

                    //Draw tick line
                    rgba = this.xAxis.getLineColor().getRGBComponents(null);
                    gl.glColor4f(rgba[0], rgba[1], rgba[2], rgba[3]);
                    gl.glLineWidth(this.xAxis.getLineWidth() * this.dpiScale);
                    gl.glBegin(GL2.GL_LINES);
                    gl.glVertex3f(v, y, zMin);
                    gl.glVertex3f(v, y1, zMin);
                    gl.glEnd();

                    //Draw tick label
                    if (this.xAxis.isDrawTickLabel()) {
                        rect = drawString(gl, tlabs.get(i), v, y1, zMin, xAlign, yAlign);
                        if (strWidth < rect.getWidth()) {
                            strWidth = (float) rect.getWidth();
                        }
                        if (strHeight < rect.getHeight()) {
                            strHeight = (float) rect.getHeight();
                        }
                    }
                }

                //Draw x axis label
                if (this.xAxis.isDrawLabel()) {
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
                        //drawString(gl, label, x1, y1, zMin, XAlign.CENTER, yAlign, 0, yShift);
                    }
                }
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
            this.yAxis.updateTickLabels();
            tlabs = this.yAxis.getTickLabels();
            if (tlabs.size() > 0) {
                float axisLen = this.toScreenLength(x, yMin, zMin, x, yMax, zMin);
                skip = getLabelGap(this.yAxis.getTickLabelFont(), tlabs, axisLen);
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
                if (this.yAxis.isDrawTickLabel()) {
                    this.updateTextRender(tlabs.get(0).getFont());
                }
                for (int i = 0; i < this.yAxis.getTickValues().length; i += skip) {
                    v = (float) this.yAxis.getTickValues()[i];
                    if (v < axesExtent.minY || v > axesExtent.maxY) {
                        continue;
                    }
                    //v = this.transform.transform_y(v);
                    if (i == tlabs.size()) {
                        break;
                    }

                    //Draw tick line
                    rgba = this.yAxis.getLineColor().getRGBComponents(null);
                    gl.glColor4f(rgba[0], rgba[1], rgba[2], rgba[3]);
                    gl.glLineWidth(this.yAxis.getLineWidth() * this.dpiScale);
                    gl.glBegin(GL2.GL_LINES);
                    gl.glVertex3f(x, v, zMin);
                    gl.glVertex3f(x1, v, zMin);
                    gl.glEnd();

                    //Draw tick label
                    if (this.yAxis.isDrawTickLabel()) {
                        rect = drawString(gl, tlabs.get(i), x1, v, zMin, xAlign, yAlign);
                        if (strWidth < rect.getWidth()) {
                            strWidth = (float) rect.getWidth();
                        }
                        if (strHeight < rect.getHeight()) {
                            strHeight = (float) rect.getHeight();
                        }
                    }
                }

                //Draw y axis label
                if (this.yAxis.isDrawLabel()) {
                    ChartText label = this.yAxis.getLabel();
                    if (label != null) {
                        this.updateTextRender(label.getFont());
                        strWidth += this.tickSpace;
                        float angle = this.toScreenAngle(x, yMin, zMin, x, yMax, zMin);
                        angle = x > center.x ? 270 - angle : 90 - angle;
                        float yShift = Math.min(-strWidth, -strWidth);
                        if (this.angleX <= -120) {
                            yShift = -yShift;
                        }
                        float y1 = (yMin + yMax) / 2;
                        drawString(gl, label, x1, y1, zMin, XAlign.CENTER, yAlign, angle, 0, yShift);
                        //drawString(gl, label, x1, y1, zMin, XAlign.CENTER, yAlign, 0, yShift);
                    }
                }
            }
        }

        //Draw z axis
        if (this.displayZ) {
            PointF loc = new PointF();
            if (this.angleY < 90) {
                loc = new PointF((float) axesExtent.minX, (float) axesExtent.maxY);
            } else if (this.angleY < 180) {
                loc = new PointF((float) axesExtent.maxX, (float) axesExtent.maxY);
            } else if (this.angleY < 270) {
                loc = new PointF((float) axesExtent.maxX, (float) axesExtent.minY);
            } else {
                loc = new PointF((float) axesExtent.minX, (float) axesExtent.minY);
            }
            drawZAxis(gl, loc);
        }
        gl.glDepthFunc(GL2.GL_LEQUAL);
    }

    protected void drawZAxis(GL2 gl, PointF loc) {
        float[] rgba;
        float x, y, v;
        int skip;
        XAlign xAlign;
        YAlign yAlign;
        Rectangle2D rect;
        float strWidth, strHeight;
        Vector3f center = this.transform.getCenter();

        x = loc.X;
        y = loc.Y;
        float zMin = (float) axesExtent.minZ;
        float zMax = (float) axesExtent.maxZ;
        /*x = this.transform.transform_x(loc.X);
        y = this.transform.transform_y(loc.Y);
        float zMin = this.transform.transform_z((float) axesExtent.minZ);
        float zMax = this.transform.transform_z((float) axesExtent.maxZ);*/

        //z axis line
        rgba = this.zAxis.getLineColor().getRGBComponents(null);
        gl.glColor4f(rgba[0], rgba[1], rgba[2], rgba[3]);
        gl.glLineWidth(this.zAxis.getLineWidth() * this.dpiScale);
        gl.glBegin(GL2.GL_LINES);
        gl.glVertex3f(x, y, zMin);
        gl.glVertex3f(x, y, zMax);
        gl.glEnd();

        //z axis ticks
        this.zAxis.updateTickLabels();
        java.util.List<ChartText> tlabs = this.zAxis.getTickLabels();
        float axisLen = this.toScreenLength(x, y, zMin, x, y, zMax);
        skip = getLabelGap(this.zAxis.getTickLabelFont(), tlabs, axisLen);
        float x1 = x;
        float y1 = y;
        float yTickLen = this.zAxis.getTickLength() * this.lenScale * transform.getYLength() / 2;
        float xTickLen = this.zAxis.getTickLength() * this.lenScale * transform.getXLength() / 2;
        if (x < center.x) {
            if (y > center.y) {
                y1 += yTickLen;
            } else {
                x1 -= xTickLen;
            }
        } else {
            if (y > center.y) {
                x1 += xTickLen;
            } else {
                y1 -= yTickLen;
            }
        }
        xAlign = XAlign.RIGHT;
        yAlign = YAlign.CENTER;
        strWidth = 0.0f;
        if (this.zAxis.isDrawTickLabel()) {
            this.updateTextRender(tlabs.get(0).getFont());
        }
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
            if (this.zAxis.isDrawTickLabel()) {
                rect = drawString(gl, tlabs.get(i), x1, y1, v, xAlign, yAlign, -this.tickSpace, 0);
                if (strWidth < rect.getWidth()) {
                    strWidth = (float) rect.getWidth();
                }
            }
        }

        //Draw z axis label
        if (this.zAxis.isDrawLabel()) {
            ChartText label = this.zAxis.getLabel();
            if (label != null) {
                this.updateTextRender(label.getFont());
                float yShift = strWidth + this.tickSpace * 3;
                float z1 = (zMax + zMin) * 0.5f;
                drawString(gl, label, x1, y1, z1, XAlign.CENTER, YAlign.BOTTOM, 90.f, 0, yShift);
            }
        }
    }

    protected void drawZAxis(GL2 gl, ZAxisOption zAxisOption) {
        Matrix4f mvMatrix = toMatrix(mvmatrix);
        gl.glPushMatrix();

        float[] rgba;
        float x, y, v;
        int skip;
        XAlign xAlign;
        YAlign yAlign;
        Rectangle2D rect;
        float strWidth, strHeight;

        PointF loc = zAxisOption.getLocation();
        boolean left = zAxisOption.isLeft();

        x = loc.X;
        y = loc.Y;
        float zMin = (float) axesExtent.minZ;
        float zMax = (float) axesExtent.maxZ;
        /*x = this.transform.transform_x(loc.X);
        y = this.transform.transform_y(loc.Y);
        float zMin = this.transform.transform_z((float) axesExtent.minZ);
        float zMax = this.transform.transform_z((float) axesExtent.maxZ);*/

        //z axis line
        rgba = this.zAxis.getLineColor().getRGBComponents(null);
        gl.glColor4f(rgba[0], rgba[1], rgba[2], rgba[3]);
        gl.glLineWidth(this.zAxis.getLineWidth() * this.dpiScale);
        gl.glBegin(GL2.GL_LINES);
        gl.glVertex3f(x, y, zMin);
        gl.glVertex3f(x, y, zMax);
        gl.glEnd();

        float axisLen = this.toScreenLength(x, y, zMin, x, y, zMax);

        //Load identity
        gl.glLoadIdentity();
        this.updateMatrix(gl);

        //z axis ticks
        Vector3f xyz, xyz1;
        this.zAxis.updateTickLabels();
        java.util.List<ChartText> tlabs = this.zAxis.getTickLabels();
        skip = getLabelGap(this.zAxis.getTickLabelFont(), tlabs, axisLen);
        float tickLen = this.zAxis.getTickLength() * this.lenScale;
        yAlign = YAlign.CENTER;
        strWidth = 0.0f;
        for (int i = 0; i < this.zAxis.getTickValues().length; i += skip) {
            v = (float) this.zAxis.getTickValues()[i];
            if (v < axesExtent.minZ || v > axesExtent.maxZ) {
                continue;
            }
            //v = this.transform.transform_z(v);
            if (i == tlabs.size()) {
                break;
            }

            xyz = new Vector3f(x, y, v);
            mvMatrix.transformPosition(xyz);
            xyz1 = new Vector3f(xyz.x, xyz.y, xyz.z);
            float xShift;
            if (left) {
                xyz1.x -= tickLen;
                xAlign = XAlign.RIGHT;
                xShift = -this.tickSpace;
            } else {
                xyz1.x += tickLen;
                xAlign = XAlign.LEFT;
                xShift = this.tickSpace;
            }

            //Draw tick line
            rgba = this.zAxis.getLineColor().getRGBComponents(null);
            gl.glColor4f(rgba[0], rgba[1], rgba[2], rgba[3]);
            gl.glLineWidth(this.zAxis.getLineWidth() * this.dpiScale);
            gl.glBegin(GL2.GL_LINES);
            gl.glVertex3f(xyz.x, xyz.y, xyz.z);
            gl.glVertex3f(xyz1.x, xyz1.y, xyz1.z);
            gl.glEnd();

            //Draw tick label
            rect = drawString(gl, tlabs.get(i), xyz1.x, xyz1.y, xyz1.z, xAlign, yAlign, xShift, 0);
            if (strWidth < rect.getWidth()) {
                strWidth = (float) rect.getWidth();
            }
        }

        //Draw z axis label
        ChartText label = this.zAxis.getLabel();
        if (label != null) {
            xyz = new Vector3f(x, y, 0);
            mvMatrix.transformPosition(xyz);
            if (left) {
                xyz.x -= tickLen;
                float yShift = strWidth + this.tickSpace * 3;
                drawString(gl, label, xyz.x, xyz.y, xyz.z, XAlign.CENTER, YAlign.BOTTOM, 90.f, 0, yShift);
            } else {
                xyz.x += tickLen;
                float yShift = -(strWidth + this.tickSpace * 3);
                drawString(gl, label, xyz.x, xyz.y, xyz.z, XAlign.CENTER, YAlign.TOP, 90.f, 0, yShift);
            }
        }

        gl.glPopMatrix();
        updateMatrix(gl);
    }

    protected void drawAllZAxis(GL2 gl) {
        for (ZAxisOption zAxisOption : this.zAxisLocations) {
            drawZAxis(gl, zAxisOption);
        }
    }

    void updateTextRender(Font font) {
        boolean newTR = false;
        if (this.textRenderer == null) {
            newTR = true;
        } else {
            if (!this.textRenderer.getFont().equals(font)) {
                //this.textRenderer.dispose();
                newTR = true;
            }
        }
        if (newTR) {
            if (this.dpiScale == 1) {
                textRenderer = new TextRenderer(font, true, true);
            } else {
                textRenderer = new TextRenderer(new Font(font.getFontName(), font.getStyle(),
                        (int) (font.getSize() * this.dpiScale)), true, true);
            }
        }
    }

    Rectangle2D drawString(GL2 gl, ChartText text, float vx, float vy, float vz, XAlign xAlign, YAlign yAlign) {
        return drawString(gl, text, vx, vy, vz, xAlign, yAlign, 0, 0);
    }

    Rectangle2D drawString(GL2 gl, ChartText text, float vx, float vy, float vz,
                           XAlign xAlign, YAlign yAlign, float xShift, float yShift) {
        return drawString(gl, text.getText(), text.getFont(), text.getColor(), vx,
                vy, vz, xAlign, yAlign, xShift, yShift);
    }

    Rectangle2D drawString(GL2 gl, String str, Font font, Color color, float vx, float vy, float vz,
                           XAlign xAlign, YAlign yAlign) {
        return drawString(gl, str, font, color, vx, vy, vz, xAlign, yAlign, 0, 0);
    }

    Rectangle2D drawString(GL2 gl, String str, Font font, Color color, float vx, float vy, float vz,
                           XAlign xAlign, YAlign yAlign, float xShift, float yShift) {
        if (Draw.getStringType(str) == StringType.LATEX) {
            return drawLaTex(gl, str, font, color, vx, vy, vz, xAlign, yAlign, xShift, yShift);
        }

        //Get screen coordinates
        Vector2f coord = this.toScreen(vx, vy, vz);
        float x = coord.x;
        float y = coord.y;

        //Rendering text string
        textRenderer.beginRendering(this.width, this.height);
        textRenderer.setColor(color);
        textRenderer.setSmoothing(true);
        Rectangle2D rect = textRenderer.getBounds(str.subSequence(0, str.length()));
        switch (xAlign) {
            case CENTER:
                x -= rect.getWidth() * 0.5;
                break;
            case RIGHT:
                x -= rect.getWidth();
                break;
        }
        switch (yAlign) {
            case CENTER:
                y -= rect.getHeight() * 0.3;
                break;
            case TOP:
                y -= rect.getHeight();
                break;
        }
        textRenderer.draw(str, (int) (x + xShift), (int) (y + yShift));
        textRenderer.endRendering();

        return rect;
    }

    Rectangle2D drawLaTex(GL2 gl, String str, Font font, Color color, float vx, float vy, float vz,
                           XAlign xAlign, YAlign yAlign, float xShift, float yShift) {
        //Get screen coordinates
        Vector2f coord = this.toScreen(vx, vy, vz);
        float x = coord.x;
        float y = coord.y;

        return drawLaTex(gl, str, font, color, x, y, xAlign, yAlign, xShift, yShift);
    }

    Rectangle2D drawLaTex(GL2 gl, String str, Font font, Color color, float x, float y,
                          XAlign xAlign, YAlign yAlign, float xShift, float yShift) {
        // create a formula
        TeXFormula formula = new TeXFormula(str);

        // render the formula to an icon of the same size as the formula.
        TeXIcon icon = formula.createTeXIcon(TeXConstants.STYLE_TEXT, font.getSize());

        // insert a border
        icon.setInsets(new Insets(5, 5, 5, 5));

        BufferedImage image = new BufferedImage(icon.getIconWidth(), icon.getIconHeight(),
                BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = image.createGraphics();

        //Dimension dim = Draw.getStringDimension(str, g2);
        Dimension dim = new Dimension(icon.getIconWidth(), icon.getIconHeight());
        switch (xAlign) {
            case CENTER:
                x -= dim.width * 0.5;
                break;
            case RIGHT:
                x -= dim.width;
                break;
        }
        switch (yAlign) {
            case CENTER:
                y -= dim.height * 0.3;
                break;
            case TOP:
                y -= dim.height;
                break;
        }

        icon.setForeground(color);
        x += xShift;
        y += yShift;
        icon.paintIcon(null, g2, 0, 0);

        Rectangle2D rect = new Rectangle2D.Float(x, y, icon.getIconWidth(), icon.getIconHeight());

        final int attribBits =
                GL2.GL_ENABLE_BIT | GL2.GL_TEXTURE_BIT | GL.GL_COLOR_BUFFER_BIT |
                        (GL.GL_DEPTH_BUFFER_BIT | GL2.GL_TRANSFORM_BIT);
        gl.glPushAttrib(attribBits);
        gl.glDisable(GL.GL_DEPTH_TEST);
        gl.glDisable(GL.GL_CULL_FACE);
        gl.glMatrixMode(GL2.GL_PROJECTION);
        gl.glPushMatrix();
        gl.glLoadIdentity();
        glu.gluOrtho2D(0, width, 0, height);
        gl.glMatrixMode(GL2.GL_MODELVIEW);
        gl.glPushMatrix();
        gl.glLoadIdentity();
        gl.glMatrixMode(GL.GL_TEXTURE);
        gl.glPushMatrix();
        gl.glLoadIdentity();

        Texture texture = AWTTextureIO.newTexture(gl.getGLProfile(), image, true);
        drawTexture(gl, texture, rect);

        gl.glMatrixMode(GL2.GL_PROJECTION);
        gl.glPopMatrix();
        gl.glMatrixMode(GL2.GL_MODELVIEW);
        gl.glPopMatrix();
        gl.glMatrixMode(GL.GL_TEXTURE);
        gl.glPopMatrix();
        gl.glPopAttrib();

        return rect;
    }

    Rectangle2D drawString(GL2 gl, ChartText text, float vx, float vy, float vz,
                           XAlign xAlign, YAlign yAlign, float angle) {
        return drawString(gl, text.getText(), text.getFont(), text.getColor(), vx, vy, vz, xAlign, yAlign, angle,
                (float)text.getXShift(), (float)text.getYShift());
    }

    Rectangle2D drawString(GL2 gl, ChartText text, float vx, float vy, float vz,
                           XAlign xAlign, YAlign yAlign, float angle, float xShift, float yShift) {
        return drawString(gl, text.getText(), text.getFont(), text.getColor(), vx, vy,
                vz, xAlign, yAlign, angle, (float)text.getXShift() + xShift, (float)text.getYShift() + yShift);
    }

    Rectangle2D drawString(GL2 gl, String str, Font font, Color color, float vx, float vy, float vz,
                           XAlign xAlign, YAlign yAlign, float angle) {
        return drawString(gl, str, font, color, vx, vy, vz, xAlign, yAlign, angle, 0, 0);
    }

    Rectangle2D drawString(GL2 gl, String str, Font font, Color color, float vx, float vy, float vz,
                           XAlign xAlign, YAlign yAlign, float angle, float xShift, float yShift) {
        //Get screen coordinates
        Vector2f coord = this.toScreen(vx, vy, vz);
        float x = coord.x;
        float y = coord.y;

        if (Draw.getStringType(str) == StringType.LATEX) {
            return drawLaTex(gl, str, font, color, x, y, xAlign, yAlign, angle, xShift, yShift);
        }

        //Rendering text string
        textRenderer.beginRendering(this.width, this.height);
        textRenderer.setColor(color);
        textRenderer.setSmoothing(true);
        Rectangle2D rect = textRenderer.getBounds(str.subSequence(0, str.length()));
        gl.glMatrixMode(GL2.GL_MODELVIEW);
        gl.glPushMatrix();
        gl.glLoadIdentity();
        gl.glTranslatef(x, y, 0.0f);
        if (angle != 0) {
            gl.glRotatef(angle, 0.0f, 0.0f, 1.0f);
        }
        x = 0;
        y = 0;
        switch (xAlign) {
            case CENTER:
                x -= rect.getWidth() * 0.5;
                break;
            case RIGHT:
                x -= rect.getWidth();
                break;
        }
        switch (yAlign) {
            case CENTER:
                y -= rect.getHeight() * 0.5;
                break;
            case TOP:
                y -= rect.getHeight();
                break;
        }
        x += xShift;
        y += yShift;
        textRenderer.draw(str, (int) x, (int) y);
        textRenderer.endRendering();
        gl.glPopMatrix();

        return rect;
    }

    Rectangle2D drawLaTex(GL2 gl, String str, Font font, Color color, float x, float y,
                          XAlign xAlign, YAlign yAlign, float angle, float xShift, float yShift) {
        // create a formula
        TeXFormula formula = new TeXFormula(str);

        // render the formula to an icon of the same size as the formula.
        TeXIcon icon = formula.createTeXIcon(TeXConstants.STYLE_TEXT, font.getSize2D());

        // insert a border
        icon.setInsets(new Insets(5, 5, 5, 5));

        BufferedImage image = new BufferedImage(icon.getIconWidth(), icon.getIconHeight(),
                BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = image.createGraphics();

        final int attribBits =
                GL2.GL_ENABLE_BIT | GL2.GL_TEXTURE_BIT | GL.GL_COLOR_BUFFER_BIT |
                        (GL.GL_DEPTH_BUFFER_BIT | GL2.GL_TRANSFORM_BIT);
        gl.glPushAttrib(attribBits);
        gl.glDisable(GL.GL_DEPTH_TEST);
        gl.glDisable(GL.GL_CULL_FACE);
        gl.glMatrixMode(GL2.GL_PROJECTION);
        gl.glPushMatrix();
        gl.glLoadIdentity();
        glu.gluOrtho2D(0, width, 0, height);
        gl.glMatrixMode(GL2.GL_MODELVIEW);
        gl.glPushMatrix();
        gl.glLoadIdentity();
        gl.glTranslatef(x, y, 0.0f);
        if (angle != 0) {
            gl.glRotatef(angle, 0.0f, 0.0f, 1.0f);
        }
        x = 0;
        y = 0;

        //Dimension dim = Draw.getStringDimension(str, g2);
        Dimension dim = new Dimension(icon.getIconWidth(), icon.getIconHeight());
        switch (xAlign) {
            case CENTER:
                x -= dim.width * 0.5;
                break;
            case RIGHT:
                x -= dim.width;
                break;
        }
        switch (yAlign) {
            case CENTER:
                y -= dim.height * 0.3;
                break;
            case TOP:
                y -= dim.height;
                break;
        }

        icon.setForeground(color);
        x += xShift;
        y += yShift;
        icon.paintIcon(null, g2, 0, 0);

        Rectangle2D rect = new Rectangle2D.Float(x, y, icon.getIconWidth(), icon.getIconHeight());

        gl.glMatrixMode(GL.GL_TEXTURE);
        gl.glPushMatrix();
        gl.glLoadIdentity();

        Texture texture = AWTTextureIO.newTexture(gl.getGLProfile(), image, true);
        drawTexture(gl, texture, rect, angle);

        gl.glMatrixMode(GL2.GL_PROJECTION);
        gl.glPopMatrix();
        gl.glMatrixMode(GL2.GL_MODELVIEW);
        gl.glPopMatrix();
        gl.glMatrixMode(GL.GL_TEXTURE);
        gl.glPopMatrix();
        gl.glPopAttrib();

        return rect;
    }

    Rectangle2D drawString3D(GL2 gl, ChartText3D text3D, float vx, float vy, float vz) {
        return drawString3D(gl, text3D.getText(), text3D.getColor(), vx, vy, vz);
    }

    Rectangle2D drawString3D(GL2 gl, String str, Color color, float vx, float vy, float vz) {
        //Rendering text string
        float scale = 0.5f;
        textRenderer.begin3DRendering();
        textRenderer.setColor(color);
        textRenderer.setSmoothing(true);
        Rectangle2D rect = textRenderer.getBounds(str.subSequence(0, str.length()));
        textRenderer.draw3D(str, vx, vy, vz, scale);
        textRenderer.end3DRendering();

        return rect;
    }

    void drawTitle() {
        if (title != null) {
            float x = (float) (this.width / 2.0f);
            float y = this.height;
            if (Draw.getStringType(title.getText()) == StringType.LATEX) {
                if (gl == null) {
                    gl = GLContext.getCurrentGL().getGL2();
                }
                drawLaTex(gl, title.getText(), title.getFont(), title.getColor(),
                        x, y, XAlign.CENTER, YAlign.TOP, 0, 0);
            } else {
                //Rendering text string
                Font font = title.getFont();
                this.updateTextRender(font);
                textRenderer.beginRendering(this.width, this.height);
                textRenderer.setColor(title.getColor());
                textRenderer.setSmoothing(true);
                Rectangle2D rect = textRenderer.getBounds(title.getText().subSequence(0, title.getText().length()));
                x = (float) (this.width / 2.0f) - (float) rect.getWidth() / 2.0f;
                y = this.height - (float) rect.getHeight();
                textRenderer.draw(title.getText(), (int) x, (int) y);
                textRenderer.endRendering();
            }
        }
    }

    protected void drawGraphics(GL2 gl, Graphic graphic) {
        boolean lightEnabled = this.lighting.isEnable();
        if (graphic instanceof GraphicCollection3D) {
            if (lightEnabled && !((GraphicCollection3D)graphic).isUsingLight()) {
                this.lighting.stop(gl);
            }
        }

        if (graphic instanceof MeshGraphic) {
            if (!this.renderMap.containsKey(graphic)) {
                renderMap.put(graphic, new MeshRender(gl, (MeshGraphic) graphic));
            }
            MeshRender meshRender = (MeshRender) renderMap.get(graphic);
            meshRender.setTransform(this.transform, this.alwaysUpdateBuffers);
            meshRender.setOrthographic(this.orthographic);
            meshRender.setLighting(this.lighting);
            meshRender.updateMatrix();
            meshRender.draw();
        } else if (graphic instanceof ParticleGraphics) {
            //this.drawParticles(gl, (ParticleGraphics) graphic);
            if (!this.renderMap.containsKey(graphic)) {
                renderMap.put(graphic, new PointRender(gl, (GraphicCollection3D) graphic));
            }
            PointRender pointRender = (PointRender) renderMap.get(graphic);
            pointRender.setTransform(this.transform, this.alwaysUpdateBuffers);
            pointRender.setOrthographic(this.orthographic);
            pointRender.setLighting(this.lighting);
            pointRender.updateMatrix();
            pointRender.draw();
        } else if (graphic instanceof TriMeshGraphic) {
            if (graphic instanceof Model) {
                if (!this.renderMap.containsKey(graphic)) {
                    renderMap.put(graphic, new ModelRender(gl, (Model) graphic));
                }
                ModelRender modelRender = (ModelRender) renderMap.get(graphic);
                modelRender.setTransform(this.transform, this.alwaysUpdateBuffers);
                modelRender.setOrthographic(this.orthographic);
                modelRender.setLighting(this.lighting);
                modelRender.updateMatrix();
                modelRender.setRotateModelView(this.modelViewMatrixR);
                modelRender.draw();
            } else {
                if (!this.renderMap.containsKey(graphic)) {
                    renderMap.put(graphic, new TriMeshRender(gl, (TriMeshGraphic) graphic));
                }
                TriMeshRender triMeshRender = (TriMeshRender) renderMap.get(graphic);
                triMeshRender.setTransform(this.transform, this.alwaysUpdateBuffers);
                triMeshRender.setOrthographic(this.orthographic);
                triMeshRender.setLighting(this.lighting);
                triMeshRender.updateMatrix();
                triMeshRender.draw();
            }
        } else if (graphic instanceof VolumeGraphic) {
            try {
                if (this.clipPlane)
                    this.disableClipPlane(gl);
                if (!this.renderMap.containsKey(graphic)) {
                    renderMap.put(graphic, new VolumeRender(gl, (VolumeGraphic) graphic));
                }
                VolumeRender volumeRender = (VolumeRender) renderMap.get(graphic);
                volumeRender.setTransform(this.transform, this.alwaysUpdateBuffers);
                volumeRender.setOrthographic(this.orthographic);
                volumeRender.updateMatrix();
                volumeRender.draw();
                if (this.clipPlane)
                    this.enableClipPlane(gl);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            boolean isDraw = true;
            if (graphic instanceof GraphicCollection3D) {
                if (graphic.getNumGraphics() == 0) {
                    isDraw = false;
                } else {
                    GraphicCollection3D gg = (GraphicCollection3D) graphic;
                    if (gg.isAllQuads()) {
                        this.drawQuadsPolygons(gl, gg);
                        isDraw = false;
                    } else if (gg.isAllTriangle()) {
                        this.drawTrianglePolygons(gl, gg);
                        isDraw = false;
                    }
                }
            }
            if (isDraw) {
                switch (graphic.getGraphicN(0).getShape().getShapeType()) {
                    case POINT_Z:
                        if (!this.renderMap.containsKey(graphic)) {
                            renderMap.put(graphic, new PointRender(gl, (GraphicCollection3D) graphic));
                        }
                        PointRender pointRender = (PointRender) renderMap.get(graphic);
                        pointRender.setTransform(this.transform, this.alwaysUpdateBuffers);
                        pointRender.setOrthographic(this.orthographic);
                        pointRender.setLighting(this.lighting);
                        pointRender.updateMatrix();
                        pointRender.setRotateModelView(this.modelViewMatrixR);
                        pointRender.draw();
                        break;
                    case POLYLINE_Z:
                        if (graphic.getGraphicN(0).getShape() instanceof PipeShape) {
                            if (!this.renderMap.containsKey(graphic)) {
                                renderMap.put(graphic, new PipeRender(gl, (GraphicCollection3D) graphic));
                            }
                            PipeRender pipeRender = (PipeRender) renderMap.get(graphic);
                            pipeRender.setTransform(this.transform, this.alwaysUpdateBuffers);
                            pipeRender.setOrthographic(this.orthographic);
                            pipeRender.setLighting(this.lighting);
                            pipeRender.updateMatrix();
                            pipeRender.setRotateModelView(this.modelViewMatrixR);
                            pipeRender.draw();
                        } else {
                            if (!this.renderMap.containsKey(graphic)) {
                                renderMap.put(graphic, new LineRender(gl, (GraphicCollection3D) graphic));
                            }
                            LineRender lineRender = (LineRender) renderMap.get(graphic);
                            lineRender.setTransform(this.transform, this.alwaysUpdateBuffers);
                            lineRender.setOrthographic(this.orthographic);
                            lineRender.setLighting(this.lighting);
                            lineRender.updateMatrix();
                            lineRender.setRotateModelView(this.modelViewMatrixR);
                            lineRender.draw();
                        }
                        break;
                    case WIND_ARROW:
                        if (!this.renderMap.containsKey(graphic)) {
                            renderMap.put(graphic, new QuiverRender(gl, (GraphicCollection3D) graphic));
                        }
                        QuiverRender quiverRender = (QuiverRender) renderMap.get(graphic);
                        quiverRender.setTransform(this.transform, this.alwaysUpdateBuffers);
                        quiverRender.setOrthographic(this.orthographic);
                        quiverRender.setLighting(this.lighting);
                        quiverRender.updateMatrix();
                        quiverRender.setRotateModelView(this.modelViewMatrixR);
                        quiverRender.draw();
                        break;
                    default:
                        for (int i = 0; i < graphic.getNumGraphics(); i++) {
                            Graphic gg = graphic.getGraphicN(i);
                            this.drawGraphic(gl, gg);
                        }
                        break;
                }
            }
        }

        if (graphic instanceof GraphicCollection3D) {
            if (lightEnabled && !((GraphicCollection3D)graphic).isUsingLight()) {
                this.lighting.start(gl);
            }
        }
    }

    protected void drawGraphic(GL2 gl, Graphic graphic) {
        Shape shape = graphic.getGraphicN(0).getShape();
        switch (shape.getShapeType()) {
            case POINT:
            case POINT_Z:
                this.drawPoint(gl, graphic);
                break;
            case TEXT:
                if (this.clipPlane)
                    this.disableClipPlane(gl);
                this.drawText(gl, (ChartText3D) shape);
                if (this.clipPlane)
                    this.enableClipPlane(gl);
                break;
            case POLYLINE:
            case POLYLINE_Z:
                /*ColorBreak cb = graphic.getLegend();
                if (cb instanceof StreamlineBreak) {
                    if (shape instanceof PipeShape) {
                        this.drawPipeStreamline(gl, graphic);
                    } else {
                        this.drawStreamline(gl, graphic);
                    }
                } else if (cb instanceof ColorBreakCollection) {
                    if (((ColorBreakCollection) cb).get(0) instanceof StreamlineBreak) {
                        if (shape instanceof PipeShape) {
                            this.drawPipeStreamline(gl, graphic);
                        } else {
                            this.drawStreamline(gl, graphic);
                        }
                    } else {
                        if (shape instanceof PipeShape) {
                            this.drawPipe(gl, graphic);
                        } else {
                            this.drawLineString(gl, graphic);
                        }
                    }
                } else {
                    if (shape instanceof PipeShape) {
                        this.drawPipe(gl, graphic);
                    } else {
                        this.drawLineString(gl, graphic);
                    }
                }*/
                break;
            case POLYGON:
            case POLYGON_Z:
                this.drawPolygonShape(gl, graphic);
                break;
            case WIND_ARROW:
                //this.drawWindArrow(gl, graphic);
                break;
            case CUBIC:
                this.drawCubic(gl, graphic);
                break;
            case CYLINDER:
                this.drawCylinder(gl, graphic);
                break;
            case IMAGE:
                this.drawImage(gl, graphic);
                break;
            case TEXTURE:
                this.drawTexture(gl, graphic);
                break;
        }
    }

    protected void drawText(GL2 gl, ChartText3D text) {
        Vector3f xyz = new Vector3f((float) text.getX(), (float) text.getY(), (float) text.getZ());
        this.updateTextRender(text.getFont());
        if (text.isDraw3D()) {
            this.drawString3D(gl, text, xyz.x, xyz.y, xyz.z);
        } else {
            this.drawString(gl, text, xyz.x, xyz.y, xyz.z, text.getXAlign(), text.getYAlign());
        }
    }

    protected void drawText3D(GL2 gl, ChartText3D text) {
        Vector3f xyz = new Vector3f((float) text.getX(), (float) text.getY(), (float) text.getZ());
        this.drawString3D(gl, text, xyz.x, xyz.y, xyz.z);
    }

    private void drawPoint(GL2 gl, Graphic graphic) {
        boolean isDraw = true;
        if (this.clipPlane)
            isDraw = drawExtent.intersects(graphic.getExtent());

        if (isDraw) {
            PointZShape shape = (PointZShape) graphic.getShape();
            PointBreak pb = (PointBreak) graphic.getLegend();
            float[] rgba = pb.getColor().getRGBComponents(null);
            gl.glColor4f(rgba[0], rgba[1], rgba[2], rgba[3]);
            gl.glPointSize(pb.getSize() * this.dpiScale);
            gl.glBegin(GL2.GL_POINTS);
            PointZ p = (PointZ) shape.getPoint();
            gl.glVertex3f((float) p.X, (float) p.Y, (float) p.Z);
            gl.glEnd();
        }
    }

    private void drawPoints(GL2 gl, Graphic graphic) {
        PointBreak pb = (PointBreak) graphic.getGraphicN(0).getLegend();
        gl.glPointSize(pb.getSize() * this.dpiScale);
        gl.glBegin(GL2.GL_POINTS);
        for (Graphic gg : graphic.getGraphics()) {
            PointZShape shape = (PointZShape) gg.getShape();
            pb = (PointBreak) gg.getLegend();
            float[] rgba = pb.getColor().getRGBComponents(null);
            gl.glColor4f(rgba[0], rgba[1], rgba[2], rgba[3]);
            PointZ p = (PointZ) shape.getPoint();
            gl.glVertex3f((float) p.X, (float) p.Y, (float) p.Z);
        }
        gl.glEnd();
    }

    private void drawSphere(GL2 gl, Graphic graphic) {
        boolean isDraw = true;
        if (this.clipPlane)
            isDraw = drawExtent.intersects(graphic.getExtent());

        if (isDraw) {
            PointZShape shape = (PointZShape) graphic.getShape();
            PointBreak pb = (PointBreak) graphic.getLegend();
            float[] rgba = pb.getColor().getRGBComponents(null);
            gl.glColor4fv(rgba, 0);
            gl.glPushMatrix();
            PointZ p = (PointZ) shape.getPoint();
            Vector3f xyz = new Vector3f((float) p.X, (float) p.Y, (float) p.Z);
            gl.glTranslated(xyz.x, xyz.y, xyz.z);
            GLUquadric sphere = glu.gluNewQuadric();
            glu.gluQuadricDrawStyle(sphere, GLU.GLU_FILL);
            glu.gluQuadricNormals(sphere, GLU.GLU_FLAT);
            glu.gluQuadricOrientation(sphere, GLU.GLU_OUTSIDE);
            glu.gluSphere(sphere, pb.getSize() * 0.005 * this.dpiScale, 16, 16);
            glu.gluDeleteQuadric(sphere);
            gl.glPopMatrix();
        }
    }

    private void drawSpheres(GL2 gl, Graphic graphic) {
        for (Graphic gg : graphic.getGraphics()) {
            drawSphere(gl, gg);
        }
    }

    private void drawParticles(GL2 gl, ParticleGraphics particles) {
        for (Map.Entry<Integer, java.util.List> map : particles.getParticleList()) {
            gl.glPointSize(particles.getPointSize() * this.dpiScale);
            gl.glBegin(GL2.GL_POINTS);
            for (ParticleGraphics.Particle p : (java.util.List<ParticleGraphics.Particle>)map.getValue()) {
                float[] rgba = p.rgba;
                gl.glColor4f(rgba[0], rgba[1], rgba[2], rgba[3]);
                gl.glVertex3f((float) p.x, (float) p.y, (float) p.z);
            }
            gl.glEnd();
        }
    }

    private int getTextureID(GL2 gl) {
        IntBuffer intBuffer = IntBuffer.allocate(1);
        gl.glGenTextures(1, intBuffer);
        return intBuffer.get(0);
    }

    private void drawVolume(GL2 gl, VolumeGraphic volume) throws Exception {
        gl.glDisable(GL_DEPTH_TEST);

        gl.glActiveTexture(GL_TEXTURE1);
        gl.glBindTexture(GL_TEXTURE_2D, getTextureID(gl));
        gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        gl.glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, 1, 1, 0, GL_RGBA, GL_UNSIGNED_BYTE, Buffers.newDirectByteBuffer(volume.getColors()));

        int idData = getTextureID(gl);
        gl.glActiveTexture(GL_TEXTURE0);
        gl.glBindTexture(GL_TEXTURE_3D, idData);
        gl.glTexParameteri(GL_TEXTURE_3D, GL_TEXTURE_BASE_LEVEL, 0);
        gl.glTexParameteri(GL_TEXTURE_3D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        gl.glTexParameteri(GL_TEXTURE_3D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        gl.glTexParameteri(GL_TEXTURE_3D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        gl.glTexParameteri(GL_TEXTURE_3D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        gl.glTexParameteri(GL_TEXTURE_3D, GL_TEXTURE_WRAP_R, GL_CLAMP_TO_EDGE);

        String vertexShaderCode = Utils.loadResource("/shaders/volume/vertex.vert");
        String fragmentShaderCode = Utils.loadResource("/shaders/volume/maxValue.frag");
        final Program program = new Program("volume", vertexShaderCode, fragmentShaderCode);
        try {
            program.init(gl);
        } catch (Exception e) {
            e.printStackTrace();
        }

        IntBuffer intBuffer = IntBuffer.allocate(1);
        gl.glGenBuffers(1, intBuffer);
        int vertexBuffer = intBuffer.get(0);
        gl.glBindBuffer(GL_ARRAY_BUFFER, vertexBuffer);
        float[] vertexBufferData = volume.getVertexBufferData(this.transform);
        gl.glBufferData(GL_ARRAY_BUFFER, vertexBufferData.length * Float.BYTES, Buffers.newDirectFloatBuffer(vertexBufferData), GL_STATIC_DRAW);
        program.allocateUniform(gl, "orthographic", (gl2, loc) -> {
            gl2.glUniform1i(loc, this.orthographic ? 1 : 0);
        });
        program.allocateUniform(gl, "MVP", (gl2, loc) -> {
            //gl2.glUniformMatrix4fv(loc, 1, false, this.camera.getViewProjectionMatrix().get(Buffers.newDirectFloatBuffer(16)));
            gl2.glUniformMatrix4fv(loc, 1, false, this.viewProjMatrix.get(Buffers.newDirectFloatBuffer(16)));
        });
        program.allocateUniform(gl, "iV", (gl2, loc) -> {
            //gl2.glUniformMatrix4fv(loc, 1, false, this.camera.getViewMatrix().invert().get(Buffers.newDirectFloatBuffer(16)));
            gl2.glUniformMatrix4fv(loc, 1, false, toMatrix(this.mvmatrix).invert().get(Buffers.newDirectFloatBuffer(16)));
        });
        program.allocateUniform(gl, "iP", (gl2, loc) -> {
            //gl2.glUniformMatrix4fv(loc, 1, false, this.camera.getProjectionMatrix().invert().get(Buffers.newDirectFloatBuffer(16)));
            gl2.glUniformMatrix4fv(loc, 1, false, toMatrix(this.projmatrix).invert().get(Buffers.newDirectFloatBuffer(16)));
        });
        program.allocateUniform(gl, "viewSize", (gl2, loc) -> {
            gl2.glUniform2f(loc, this.width, this.height);
        });
        int sampleCount = 512;
        program.allocateUniform(gl, "depthSampleCount", (gl2, loc) -> {
            gl2.glUniform1i(loc, sampleCount);
        });
        program.allocateUniform(gl, "tex", (gl2, loc) -> {
            gl2.glUniform1i(loc, 0);
        });
        program.allocateUniform(gl, "colorMap", (gl2, loc) -> {
            gl2.glUniform1i(loc, 1);
        });
        float[] aabbMin = volume.getAabbMin();
        float[] aabbMax = volume.getAabbMax();
        program.allocateUniform(gl, "aabbMin", (gl2, loc) -> {
            gl2.glUniform3f(loc, aabbMin[0], aabbMin[1], aabbMin[2]);
        });
        program.allocateUniform(gl, "aabbMax", (gl2, loc) -> {
            gl2.glUniform3f(loc, aabbMax[0], aabbMax[1], aabbMax[2]);
        });

        program.use(gl);
        program.setUniforms(gl);

        gl.glActiveTexture(GL_TEXTURE1);
        gl.glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, volume.getColorNum(), 1, 0, GL_RGBA, GL_UNSIGNED_BYTE,
                Buffers.newDirectByteBuffer(volume.getColors()).rewind());

        gl.glActiveTexture(GL_TEXTURE0);
        gl.glBindTexture(GL_TEXTURE_3D, idData);
        gl.glPixelStorei(GL_UNPACK_ALIGNMENT, 1);
        gl.glTexImage3D(
                GL_TEXTURE_3D,  // target
                0,              // level
                GL_LUMINANCE,        // internal format
                volume.getWidth(),           // width
                volume.getHeight(),           // height
                volume.getDepth(),           // depth
                0,              // border
                GL_LUMINANCE,         // format
                GL_UNSIGNED_BYTE,       // type
                Buffers.newDirectByteBuffer(volume.getData()).rewind()           // pixel
        );

        // 1st attribute buffer : vertices
        gl.glEnableVertexAttribArray(0);
        gl.glBindBuffer(GL_ARRAY_BUFFER, vertexBuffer);
        gl.glVertexAttribPointer(
                0,                  // attribute 0. No particular reason for 0, but must match the layout in the shader.
                3,                  // size
                GL_FLOAT,           // type
                false,           // normalized?
                3 * 4,                  // stride
                0            // array buffer offset
        );

        // Draw the triangle !
        gl.glDrawArrays(GL_TRIANGLES, 0, vertexBufferData.length / 3); // Starting from vertex 0; 3 vertices total -> 1 triangle
        gl.glDisableVertexAttribArray(0);

        Program.destroyAllPrograms(gl);
        gl.glUseProgram(0);
        gl.glEnable(GL_DEPTH_TEST);
    }

    private void drawLineString(GL2 gl, Graphic graphic) {
        boolean isDraw = true;
        if (this.clipPlane)
            isDraw = drawExtent.intersects(graphic.getExtent());

        if (isDraw) {
            PolylineZShape shape = (PolylineZShape) graphic.getShape();
            ColorBreak cb = graphic.getLegend();
            if (cb.getBreakType() == BreakTypes.COLOR_BREAK_COLLECTION) {
                ColorBreakCollection cbc = (ColorBreakCollection) cb;
                Polyline line = shape.getPolylines().get(0);
                java.util.List<PointZ> ps = (java.util.List<PointZ>) line.getPointList();
                float[] rgba;
                PointZ p;
                gl.glLineWidth(((PolylineBreak) cbc.get(0)).getWidth() * this.dpiScale);
                gl.glBegin(GL2.GL_LINE_STRIP);
                for (int i = 0; i < ps.size(); i++) {
                    PolylineBreak plb = (PolylineBreak) cbc.get(i);
                    rgba = plb.getColor().getRGBComponents(null);
                    gl.glColor4f(rgba[0], rgba[1], rgba[2], rgba[3]);
                    gl.glLineWidth(plb.getWidth() * this.dpiScale);
                    p = ps.get(i);
                    gl.glVertex3f((float) p.X, (float) p.Y, (float) p.Z);
                }
                gl.glEnd();
            } else {
                PolylineBreak pb = (PolylineBreak) cb;
                float[] rgba = pb.getColor().getRGBComponents(null);
                gl.glColor4f(rgba[0], rgba[1], rgba[2], rgba[3]);
                gl.glLineWidth(pb.getWidth() * this.dpiScale);
                for (Polyline line : shape.getPolylines()) {
                    gl.glBegin(GL2.GL_LINE_STRIP);
                    java.util.List<PointZ> ps = (java.util.List<PointZ>) line.getPointList();
                    for (PointZ p : ps) {
                        gl.glVertex3f((float) p.X, (float) p.Y, (float) p.Z);
                    }
                    gl.glEnd();
                }
            }
        }
    }

    private void drawPipe(GL2 gl, Graphic graphic) {
        boolean isDraw = true;
        if (this.clipPlane)
            isDraw = drawExtent.intersects(graphic.getExtent());

        if (isDraw) {
            PipeShape shape = (PipeShape) graphic.getShape();
            ColorBreak cb = graphic.getLegend();
            shape.transform(transform);
            Pipe pipe = shape.getPipe();
            int count = pipe.getContourCount();
            if (cb.getBreakType() == BreakTypes.COLOR_BREAK_COLLECTION) {
                ColorBreakCollection cbc = (ColorBreakCollection) cb;
                float[] rgba;
                for (int i = 0; i < count - 1; i++) {
                    PolylineBreak plb = (PolylineBreak) cbc.get(i);
                    rgba = plb.getColor().getRGBComponents(null);
                    gl.glColor4f(rgba[0], rgba[1], rgba[2], rgba[3]);
                    List<Vector3f> c1 = pipe.getContour(i);
                    List<Vector3f> c2 = pipe.getContour(i+1);
                    List<Vector3f> n1 = pipe.getNormal(i);
                    List<Vector3f> n2 = pipe.getNormal(i+1);
                    gl.glBegin(GL_TRIANGLE_STRIP);
                    for(int j = 0; j < c2.size(); ++j)
                    {
                        gl.glNormal3fv(JOGLUtil.toArray(n2.get(j)), 0);
                        gl.glVertex3fv(JOGLUtil.toArray(c2.get(j)), 0);
                        gl.glNormal3fv(JOGLUtil.toArray(n1.get(j)), 0);
                        gl.glVertex3fv(JOGLUtil.toArray(c1.get(j)), 0);
                    }
                    gl.glEnd();
                }
                gl.glEnd();
            } else {
                PolylineBreak pb = (PolylineBreak) cb;
                float[] rgba = pb.getColor().getRGBComponents(null);
                gl.glColor4f(rgba[0], rgba[1], rgba[2], rgba[3]);
                for(int i = 0; i < count - 1; i++)
                {
                    List<Vector3f> c1 = pipe.getContour(i);
                    List<Vector3f> c2 = pipe.getContour(i+1);
                    List<Vector3f> n1 = pipe.getNormal(i);
                    List<Vector3f> n2 = pipe.getNormal(i+1);
                    gl.glBegin(GL_TRIANGLE_STRIP);
                    for(int j = 0; j < (int)c2.size(); ++j)
                    {
                        gl.glNormal3fv(JOGLUtil.toArray(n2.get(j)), 0);
                        gl.glVertex3fv(JOGLUtil.toArray(c2.get(j)), 0);
                        gl.glNormal3fv(JOGLUtil.toArray(n1.get(j)), 0);
                        gl.glVertex3fv(JOGLUtil.toArray(c1.get(j)), 0);
                    }
                    gl.glEnd();
                }
            }
        }
    }

    protected void drawPolygonShape(GL2 gl, Graphic graphic) {
        boolean isDraw = true;
        if (this.clipPlane)
            isDraw = drawExtent.intersects(graphic.getExtent());

        if (isDraw) {
            PolygonZShape shape = (PolygonZShape) graphic.getShape();
            PolygonBreak pb = (PolygonBreak) graphic.getLegend();
            java.util.List<PolygonZ> polygonZS = (java.util.List<PolygonZ>) shape.getPolygons();
            for (int i = 0; i < polygonZS.size(); i++) {
                PolygonZ polygonZ = polygonZS.get(i);
                if (pb.isDrawFill()) {
                    if (polygonZ instanceof TessPolygon) {
                        drawTessPolygon(gl, (TessPolygon) polygonZ, pb);
                    } else {
                        if (polygonZ.getOutLine().size() <= 5) {
                            drawConvexPolygon(gl, polygonZ, pb);
                        } else {
                            TessPolygon tessPolygon = new TessPolygon(polygonZ);
                            drawTessPolygon(gl, tessPolygon, pb);
                            polygonZS.set(i, tessPolygon);
                        }
                    }
                } else {
                    drawPolygon(gl, polygonZ, pb);
                }
            }
        }
    }

    private void drawTessPolygon(GL2 gl, TessPolygon tessPolygon, PolygonBreak aPGB) {
        if (aPGB.isDrawFill() && aPGB.getColor().getAlpha() > 0) {
            gl.glEnable(GL2.GL_POLYGON_OFFSET_FILL);
            gl.glPolygonOffset(1.0f, 1.0f);

            float[] rgba = aPGB.getColor().getRGBComponents(null);
            gl.glColor4f(rgba[0], rgba[1], rgba[2], rgba[3]);

            if (tessPolygon.getPrimitives() != null) {
                try {
                    for (Primitive primitive : tessPolygon.getPrimitives()) {
                        gl.glBegin(primitive.type);
                        for (PointZ p : primitive.vertices) {
                            gl.glVertex3f((float) p.X, (float) p.Y, (float) p.Z);
                        }
                        gl.glEnd();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        if (aPGB.isDrawOutline()) {
            float[] rgba = aPGB.getOutlineColor().getRGBComponents(null);
            gl.glLineWidth(aPGB.getOutlineSize() * this.dpiScale);
            gl.glColor4f(rgba[0], rgba[1], rgba[2], rgba[3]);
            gl.glBegin(GL2.GL_LINE_STRIP);
            PointZ p;
            for (int i = 0; i < tessPolygon.getOutLine().size(); i++) {
                p = ((java.util.List<PointZ>) tessPolygon.getOutLine()).get(i);
                gl.glVertex3f((float) p.X, (float) p.Y, (float) p.Z);
            }
            gl.glEnd();

            if (tessPolygon.hasHole()) {
                java.util.List<PointZ> newPList;
                for (int h = 0; h < tessPolygon.getHoleLines().size(); h++) {
                    gl.glBegin(GL2.GL_LINE_STRIP);
                    newPList = (java.util.List<PointZ>) tessPolygon.getHoleLines().get(h);
                    for (int j = 0; j < newPList.size(); j++) {
                        p = newPList.get(j);
                        gl.glVertex3f((float) p.X, (float) p.Y, (float) p.Z);
                    }
                    gl.glEnd();
                }
            }
            gl.glDisable(GL2.GL_POLYGON_OFFSET_FILL);
        }
    }

    private void drawPolygon(GL2 gl, PolygonZ aPG, PolygonBreak aPGB) {
        if (aPGB.isDrawFill() && aPGB.getColor().getAlpha() > 0) {
            gl.glEnable(GL2.GL_POLYGON_OFFSET_FILL);
            gl.glPolygonOffset(1.0f, 1.0f);

            float[] rgba = aPGB.getColor().getRGBComponents(null);
            gl.glColor4f(rgba[0], rgba[1], rgba[2], rgba[3]);

            try {
                TessPolygon tessPolygon = new TessPolygon(aPG);
                for (Primitive primitive : tessPolygon.getPrimitives()) {
                    gl.glBegin(primitive.type);
                    for (PointZ p : primitive.vertices) {
                        gl.glVertex3f((float) p.X, (float) p.Y, (float) p.Z);
                    }
                    gl.glEnd();
                }
                aPG = tessPolygon;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (aPGB.isDrawOutline()) {
            float[] rgba = aPGB.getOutlineColor().getRGBComponents(null);
            gl.glLineWidth(aPGB.getOutlineSize() * this.dpiScale);
            gl.glColor4f(rgba[0], rgba[1], rgba[2], rgba[3]);
            gl.glBegin(GL2.GL_LINE_STRIP);
            PointZ p;
            for (int i = 0; i < aPG.getOutLine().size(); i++) {
                p = ((java.util.List<PointZ>) aPG.getOutLine()).get(i);
                gl.glVertex3f((float) p.X, (float) p.Y, (float) p.Z);
            }
            gl.glEnd();

            if (aPG.hasHole()) {
                java.util.List<PointZ> newPList;
                gl.glBegin(GL2.GL_LINE_STRIP);
                for (int h = 0; h < aPG.getHoleLines().size(); h++) {
                    newPList = (java.util.List<PointZ>) aPG.getHoleLines().get(h);
                    for (int j = 0; j < newPList.size(); j++) {
                        p = newPList.get(j);
                        gl.glVertex3f((float) p.X, (float) p.Y, (float) p.Z);
                    }
                }
                gl.glEnd();
            }
            gl.glDisable(GL2.GL_POLYGON_OFFSET_FILL);
        }
    }

    private void drawPolygon_bak(GL2 gl, PolygonZ aPG, PolygonBreak aPGB) {
        PointZ p;
        if (aPGB.isDrawFill() && aPGB.getColor().getAlpha() > 0) {
            gl.glEnable(GL2.GL_POLYGON_OFFSET_FILL);
            gl.glPolygonOffset(1.0f, 1.0f);

            float[] rgba = aPGB.getColor().getRGBComponents(null);
            gl.glColor4f(rgba[0], rgba[1], rgba[2], rgba[3]);

            try {
                GLUtessellator tobj = glu.gluNewTess();
                //TessCallback tessCallback = new TessCallback(gl, glu);

                glu.gluTessCallback(tobj, GLU.GLU_TESS_VERTEX, tessCallback);
                glu.gluTessCallback(tobj, GLU.GLU_TESS_BEGIN, tessCallback);
                glu.gluTessCallback(tobj, GLU.GLU_TESS_END, tessCallback);
                glu.gluTessCallback(tobj, GLU.GLU_TESS_ERROR, tessCallback);
                //glu.gluTessCallback(tobj, GLU.GLU_TESS_COMBINE, tessCallback);

                //gl.glNewList(startList, GL2.GL_COMPILE);
                //gl.glShadeModel(GL2.GL_FLAT);
                glu.gluTessBeginPolygon(tobj, null);
                glu.gluTessBeginContour(tobj);
                double[] v;
                for (int i = 0; i < aPG.getOutLine().size() - 1; i++) {
                    p = ((java.util.List<PointZ>) aPG.getOutLine()).get(i);
                    v = transform.transform(p);
                    glu.gluTessVertex(tobj, v, 0, v);
                }
                glu.gluTessEndContour(tobj);
                if (aPG.hasHole()) {
                    for (int i = 0; i < aPG.getHoleLineNumber(); i++) {
                        glu.gluTessBeginContour(tobj);
                        for (int j = 0; j < aPG.getHoleLine(i).size() - 1; j++) {
                            p = ((java.util.List<PointZ>) aPG.getHoleLine(i)).get(j);
                            v = transform.transform(p);
                            glu.gluTessVertex(tobj, v, 0, v);
                        }
                        glu.gluTessEndContour(tobj);
                    }
                }
                glu.gluTessEndPolygon(tobj);
                //gl.glEndList();
                glu.gluDeleteTess(tobj);

                //gl.glCallList(startList);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (aPGB.isDrawOutline()) {
            float[] rgba = aPGB.getOutlineColor().getRGBComponents(null);
            gl.glLineWidth(aPGB.getOutlineSize() * this.dpiScale);
            gl.glColor4f(rgba[0], rgba[1], rgba[2], rgba[3]);
            gl.glBegin(GL2.GL_LINE_STRIP);
            for (int i = 0; i < aPG.getOutLine().size(); i++) {
                p = ((java.util.List<PointZ>) aPG.getOutLine()).get(i);
                gl.glVertex3f(transform.transform_x((float) p.X), transform.transform_y((float) p.Y), transform.transform_z((float) p.Z));
            }
            gl.glEnd();

            if (aPG.hasHole()) {
                java.util.List<PointZ> newPList;
                gl.glBegin(GL2.GL_LINE_STRIP);
                for (int h = 0; h < aPG.getHoleLines().size(); h++) {
                    newPList = (java.util.List<PointZ>) aPG.getHoleLines().get(h);
                    for (int j = 0; j < newPList.size(); j++) {
                        p = newPList.get(j);
                        gl.glVertex3f(transform.transform_x((float) p.X), transform.transform_y((float) p.Y), transform.transform_z((float) p.Z));
                    }
                }
                gl.glEnd();
            }
            gl.glDisable(GL2.GL_POLYGON_OFFSET_FILL);
        }
    }

    private void drawConvexPolygon(GL2 gl, PolygonZ aPG, PolygonBreak aPGB) {
        PointZ p;
        if (aPGB.isDrawFill()) {
            float[] rgba = aPGB.getColor().getRGBComponents(null);
            gl.glColor4f(rgba[0], rgba[1], rgba[2], rgba[3]);
            gl.glBegin(GL2.GL_POLYGON);
            for (int i = 0; i < aPG.getOutLine().size(); i++) {
                p = ((java.util.List<PointZ>) aPG.getOutLine()).get(i);
                gl.glVertex3f((float) p.X, (float) p.Y, (float) p.Z);
            }
            gl.glEnd();
        }

        if (aPGB.isDrawOutline()) {
            float[] rgba = aPGB.getOutlineColor().getRGBComponents(null);
            gl.glLineWidth(aPGB.getOutlineSize() * this.dpiScale);
            gl.glColor4f(rgba[0], rgba[1], rgba[2], rgba[3]);
            gl.glBegin(GL2.GL_LINE_STRIP);
            for (int i = 0; i < aPG.getOutLine().size(); i++) {
                p = ((java.util.List<PointZ>) aPG.getOutLine()).get(i);
                gl.glVertex3f((float) p.X, (float) p.Y, (float) p.Z);
            }
            gl.glEnd();
        }
    }

    private void drawQuadsPolygons(GL2 gl, GraphicCollection3D graphic) {
        PointZ p;
        for (int i = 0; i < graphic.getNumGraphics(); i++) {
            Graphic gg = graphic.getGraphicN(i);
            boolean isDraw = true;
            if (this.clipPlane)
                isDraw = drawExtent.intersects(gg.getExtent());

            if (isDraw) {
                PolygonZShape shape = (PolygonZShape) gg.getShape();
                PolygonBreak pb = (PolygonBreak) gg.getLegend();
                for (PolygonZ poly : (java.util.List<PolygonZ>) shape.getPolygons()) {
                    drawQuads(gl, poly, pb);
                }
            }
        }
    }

    private void drawQuads(GL2 gl, PolygonZ aPG, PolygonBreak aPGB) {
        PointZ p;
        float[] rgba = aPGB.getColor().getRGBComponents(null);
        if (aPGB.isDrawFill()) {
            gl.glColor4f(rgba[0], rgba[1], rgba[2], rgba[3]);
            gl.glBegin(GL2.GL_QUADS);
            for (int i = 0; i < aPG.getOutLine().size(); i++) {
                p = ((java.util.List<PointZ>) aPG.getOutLine()).get(i);
                gl.glVertex3f((float) p.X, (float) p.Y, (float) p.Z);
            }
            gl.glEnd();
        }

        if (aPGB.isDrawOutline()) {
            rgba = aPGB.getOutlineColor().getRGBComponents(null);
            gl.glLineWidth(aPGB.getOutlineSize() * this.dpiScale);
            gl.glColor4f(rgba[0], rgba[1], rgba[2], rgba[3]);
            gl.glBegin(GL2.GL_LINE_STRIP);
            for (int i = 0; i < aPG.getOutLine().size(); i++) {
                p = ((java.util.List<PointZ>) aPG.getOutLine()).get(i);
                gl.glVertex3f((float) p.X, (float) p.Y, (float) p.Z);
            }
            gl.glEnd();
        }
    }

    private void drawTrianglePolygons(GL2 gl, GraphicCollection3D graphic) {
        PointZ p;
        for (int i = 0; i < graphic.getNumGraphics(); i++) {
            Graphic gg = graphic.getGraphicN(i);
            boolean isDraw = true;
            if (this.clipPlane)
                isDraw = drawExtent.intersects(gg.getExtent());

            if (isDraw) {
                PolygonZShape shape = (PolygonZShape) gg.getShape();
                PolygonBreak pb = (PolygonBreak) gg.getLegend();
                for (PolygonZ poly : (java.util.List<PolygonZ>) shape.getPolygons()) {
                    drawTriangle(gl, poly, pb);
                }
            }
        }
    }

    private void drawTriangle(GL2 gl, PolygonZ aPG, PolygonBreak aPGB) {
        PointZ p;
        float[] rgba = aPGB.getColor().getRGBComponents(null);
        if (aPGB.isDrawFill()) {
            gl.glColor4f(rgba[0], rgba[1], rgba[2], rgba[3]);
            gl.glBegin(GL2.GL_TRIANGLES);
            for (int i = 0; i < aPG.getOutLine().size(); i++) {
                p = ((java.util.List<PointZ>) aPG.getOutLine()).get(i);
                gl.glVertex3f((float) p.X, (float) p.Y, (float) p.Z);
            }
            gl.glEnd();
        }

        if (aPGB.isDrawOutline()) {
            rgba = aPGB.getOutlineColor().getRGBComponents(null);
            gl.glLineWidth(aPGB.getOutlineSize() * this.dpiScale);
            gl.glColor4f(rgba[0], rgba[1], rgba[2], rgba[3]);
            gl.glBegin(GL2.GL_LINE_STRIP);
            for (int i = 0; i < aPG.getOutLine().size(); i++) {
                p = ((java.util.List<PointZ>) aPG.getOutLine()).get(i);
                gl.glVertex3f((float) p.X, (float) p.Y, (float) p.Z);
            }
            gl.glEnd();
        }
    }

    private void drawTriangle(GL2 gl, PointZ[] points, PolygonBreak aPGB) {
        PointZ p;
        float[] rgba = aPGB.getColor().getRGBComponents(null);
        if (aPGB.isDrawFill()) {
            gl.glEnable(GL2.GL_POLYGON_OFFSET_FILL);
            gl.glPolygonOffset(1.0f, 1.0f);
            gl.glColor4f(rgba[0], rgba[1], rgba[2], rgba[3]);
            float[] x0 = points[0].toFloatArray();
            float[] x1 = points[1].toFloatArray();
            float[] x2 = points[2].toFloatArray();
            gl.glBegin(GL2.GL_TRIANGLES);
            if (this.lighting.isEnable()) {
                float[] normal = JOGLUtil.normalize(x0, x1, x2);
                gl.glNormal3fv(normal, 0);
            }
            gl.glVertex3fv(x0, 0);
            gl.glVertex3fv(x1, 0);
            gl.glVertex3fv(x2, 0);
            gl.glEnd();
        }

        if (aPGB.isDrawOutline()) {
            rgba = aPGB.getOutlineColor().getRGBComponents(null);
            gl.glLineWidth(aPGB.getOutlineSize() * this.dpiScale);
            gl.glColor4f(rgba[0], rgba[1], rgba[2], rgba[3]);
            gl.glBegin(GL2.GL_LINE_STRIP);
            for (int i = 0; i < 3; i++) {
                p = points[i];
                gl.glVertex3f((float) p.X, (float) p.Y, (float) p.Z);
            }
            gl.glEnd();
        }
    }

    private void drawIsosurface(GL2 gl, IsosurfaceGraphics isosurface) {
        java.util.List<PointZ[]> triangles = isosurface.getTriangles();
        PolygonBreak pgb = (PolygonBreak) isosurface.getLegendBreak();
        for (PointZ[] triangle : triangles) {
            this.drawTriangle(gl, triangle, pgb);
        }
    }

    private void drawImage(GL2 gl, Graphic graphic) {
        ImageShape ishape = (ImageShape) graphic.getShape();
        BufferedImage image = ishape.getImage();
        Texture texture = AWTTextureIO.newTexture(gl.getGLProfile(), image, true);
        //Texture texture = this.imageCache.get(image);
        int idTexture = texture.getTextureObject();
        java.util.List<PointZ> coords = ishape.getCoords();

        gl.glEnable(GL2.GL_TEXTURE_2D);
        gl.glColor3f(1f, 1f, 1f);
        gl.glBindTexture(GL2.GL_TEXTURE_2D, idTexture);

        // Texture parameterization
        gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MIN_FILTER, GL2.GL_NEAREST);
        gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MAG_FILTER, GL2.GL_LINEAR);

        // Draw image
        gl.glBegin(GL2.GL_QUADS);
        // Front Face
        //gl.glTexCoord2f(0.0f, 0.0f);
        gl.glTexCoord2f(0.0f, 1.0f);
        gl.glVertex3f((float) coords.get(0).X, (float) coords.get(0).Y, (float) coords.get(0).Z);
        //gl.glTexCoord2f(1.0f, 0.0f);
        gl.glTexCoord2f(1.0f, 1.0f);
        gl.glVertex3f((float) coords.get(1).X, (float) coords.get(1).Y, (float) coords.get(1).Z);
        //gl.glTexCoord2f(1.0f, 1.0f);
        gl.glTexCoord2f(1.0f, 0.0f);
        gl.glVertex3f((float) coords.get(2).X, (float) coords.get(2).Y, (float) coords.get(2).Z);
        //gl.glTexCoord2f(0.0f, 1.0f);
        gl.glTexCoord2f(0.0f, 0.0f);
        gl.glVertex3f((float) coords.get(3).X, (float) coords.get(3).Y, (float) coords.get(3).Z);
        gl.glEnd();

        // Unbinding the texture
        gl.glBindTexture(GL2.GL_TEXTURE_2D, 0);
        gl.glDisable(GL2.GL_TEXTURE_2D);
    }

    private void drawImage(GL2 gl) throws IOException {
        File im = new File("D:\\Temp\\image\\lenna.jpg ");
        BufferedImage image = ImageIO.read(im);
        Texture t = AWTTextureIO.newTexture(gl.getGLProfile(), image, true);
        //Texture t = TextureIO.newTexture(im, true);
        //Texture t = this.imageCache.get(image);
        int idTexture = t.getTextureObject(gl);

        gl.glColor3f(1f, 1f, 1f);
        gl.glBindTexture(GL2.GL_TEXTURE_2D, idTexture);

//        // Texture parameterization
//        gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MIN_FILTER, GL2.GL_LINEAR_MIPMAP_LINEAR);
//        gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MAG_FILTER, GL2.GL_LINEAR);
        // Draw image
        gl.glBegin(GL2.GL_QUADS);
        // Front Face
        gl.glTexCoord2f(0.0f, 0.0f);
        gl.glVertex3f(-1.0f, -1.0f, 1.0f);
        gl.glTexCoord2f(1.0f, 0.0f);
        gl.glVertex3f(1.0f, -1.0f, 1.0f);
        gl.glTexCoord2f(1.0f, 1.0f);
        gl.glVertex3f(1.0f, 1.0f, 1.0f);
        gl.glTexCoord2f(0.0f, 1.0f);
        gl.glVertex3f(-1.0f, 1.0f, 1.0f);
        gl.glEnd();

        // Unbinding the texture
        gl.glBindTexture(GL2.GL_TEXTURE_2D, 0);
    }

    private void drawTexture(GL2 gl, Graphic graphic) {
        TextureShape ishape = (TextureShape) graphic.getShape();
        ishape.updateTexture(gl);
        int idTexture = ishape.getTextureID();
        java.util.List<PointZ> coords = ishape.getCoords();
        int xRepeat = ishape.getXRepeat();
        int yRepeat = ishape.getYRepeat();
        float width = (float) (coords.get(1).X - coords.get(0).X);
        float height = (float) (coords.get(1).Y - coords.get(2).Y);
        width = width * (xRepeat - 1);
        height = height * (yRepeat - 1);

        gl.glEnable(GL2.GL_TEXTURE_2D);
        gl.glColor3f(1f, 1f, 1f);
        gl.glBindTexture(GL2.GL_TEXTURE_2D, idTexture);

        // Texture parameterization
        //gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MIN_FILTER, GL2.GL_NEAREST);
        gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MIN_FILTER, GL2.GL_LINEAR_MIPMAP_LINEAR);
        gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MAG_FILTER, GL2.GL_LINEAR);
        gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_WRAP_S, GL2.GL_REPEAT);
        gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_WRAP_T, GL2.GL_REPEAT);

        // Draw image
        gl.glBegin(GL2.GL_QUADS);
        // Front Face
        //gl.glTexCoord2f(0.0f, 0.0f);
        gl.glTexCoord2f(0.0f, 1.0f * yRepeat);
        gl.glVertex3f((float) coords.get(0).X, (float) coords.get(0).Y + height, (float) coords.get(0).Z);
        //gl.glTexCoord2f(1.0f, 0.0f);
        gl.glTexCoord2f(1.0f * xRepeat, 1.0f * yRepeat);
        gl.glVertex3f((float) coords.get(1).X + width, (float) coords.get(1).Y + height, (float) coords.get(1).Z);
        //gl.glTexCoord2f(1.0f, 1.0f);
        gl.glTexCoord2f(1.0f * xRepeat, 0.0f);
        gl.glVertex3f((float) coords.get(2).X + width, (float) coords.get(2).Y, (float) coords.get(2).Z);
        //gl.glTexCoord2f(0.0f, 1.0f);
        gl.glTexCoord2f(0.0f, 0.0f);
        gl.glVertex3f((float) coords.get(3).X, (float) coords.get(3).Y, (float) coords.get(3).Z);
        gl.glEnd();
        gl.glFlush();

        // Unbinding the texture
        gl.glBindTexture(GL2.GL_TEXTURE_2D, 0);
        gl.glDisable(GL2.GL_TEXTURE_2D);
    }

    private void drawTexture(GL2 gl, Texture texture, Rectangle2D rect) {
        drawTexture(gl, texture, rect, 0);
    }

    private void drawTexture(GL2 gl, Texture texture, Rectangle2D rect, float angle) {
        int idTexture = texture.getTextureObject(gl);
        int width = texture.getWidth();
        int height = texture.getHeight();

        gl.glEnable(GL2.GL_TEXTURE_2D);
        gl.glColor3f(1f, 1f, 1f);
        gl.glBindTexture(GL2.GL_TEXTURE_2D, idTexture);

        // Texture parameterization
        int filter = GL2.GL_NEAREST;
        if (angle != 0 && angle != 90) {
            filter = GL_LINEAR;
        }
        gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MIN_FILTER, filter);
        gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MAG_FILTER, filter);

        // Draw image
        gl.glBegin(GL2.GL_QUADS);
        // Front Face
        gl.glTexCoord2f(0.0f, 1.0f);
        gl.glVertex2f((float) rect.getX(), (float) rect.getY());
        gl.glTexCoord2f(1.0f, 1.0f);
        gl.glVertex2f((float) rect.getMaxX(), (float) rect.getY());
        gl.glTexCoord2f(1.0f, 0.0f);
        gl.glVertex2f((float) rect.getMaxX(), (float) rect.getMaxY());
        gl.glTexCoord2f(0.0f, 0.0f);
        gl.glVertex2f((float) rect.getX(), (float) rect.getMaxY());
        gl.glEnd();
        gl.glFlush();

        // Unbinding the texture
        gl.glBindTexture(GL2.GL_TEXTURE_2D, 0);
        gl.glDisable(GL2.GL_TEXTURE_2D);
    }

    void drawCircle(GL2 gl, float z, float radius, PolygonBreak bb) {
        drawCircle(gl, z, radius, bb, false);
    }

    void drawCircle(GL2 gl, float z, float radius, PolygonBreak bb, boolean clockwise) {
        int points = 100;
        java.util.List<float[]> vertex = new ArrayList<>();
        double angle = 0.0;
        if (clockwise) {
            for (int i = points - 1; i >= 0; i--) {
                angle = 2 * Math.PI * i / points;
                vertex.add(new float[]{(float) Math.cos(angle) * radius, (float) Math.sin(angle) * radius, z});
            }
        } else {
            for (int i = 0; i < points; i++) {
                angle = 2 * Math.PI * i / points;
                vertex.add(new float[]{(float) Math.cos(angle) * radius, (float) Math.sin(angle) * radius, z});
            }
        }

        if (bb.isDrawFill()) {
            float[] rgba = bb.getColor().getRGBComponents(null);
            gl.glColor4f(rgba[0], rgba[1], rgba[2], rgba[3]);
            gl.glBegin(GL2.GL_TRIANGLE_FAN);
            if (this.lighting.isEnable()) {
                int ii = points / 4;
                float[] normal;
                normal = JOGLUtil.normalize(vertex.get(0), vertex.get(ii), vertex.get(ii * 2));
                gl.glNormal3fv(normal, 0);
            }
            for(int i =0; i < points;i++){
                gl.glVertex3f(vertex.get(i)[0], vertex.get(i)[1], vertex.get(i)[2]);
            }
            gl.glEnd();
        }

        if (bb.isDrawOutline()) {
            float[] rgba = bb.getOutlineColor().getRGBComponents(null);
            gl.glColor4f(rgba[0], rgba[1], rgba[2], rgba[3]);
            gl.glLineWidth(bb.getOutlineSize() * this.dpiScale);
            gl.glBegin(GL2.GL_LINE_LOOP);
            for (int i = 0; i < points; i++) {
                gl.glVertex3f(vertex.get(i)[0], vertex.get(i)[1], vertex.get(i)[2]);
            }
            gl.glEnd();
        }
    }

    void drawCubic(GL2 gl, Graphic graphic) {
        boolean isDraw = true;
        if (this.clipPlane)
            isDraw = drawExtent.intersects(graphic.getExtent());

        if (isDraw) {
            CubicShape cubic = (CubicShape) graphic.getShape();
            BarBreak bb = (BarBreak) graphic.getLegend();
            java.util.List<PointZ> ps = cubic.getPoints();
            java.util.List<float[]> vertex = new ArrayList<>();
            for (PointZ p : ps) {
                vertex.add(new float[]{(float) p.X, (float) p.Y, (float) p.Z});
            }

            gl.glEnable(GL2.GL_POLYGON_OFFSET_FILL);
            gl.glPolygonOffset(1.0f, 1.0f);
            int[][] index = cubic.getIndex();
            float[] rgba = bb.getColor().getRGBComponents(null);
            gl.glColor4f(rgba[0], rgba[1], rgba[2], rgba[3]);
            gl.glBegin(GL2.GL_QUADS);
            for (int[] ii : index) {
                if (this.lighting.isEnable()) {
                    float[] normal = JOGLUtil.normalize(vertex.get(ii[0]), vertex.get(ii[1]), vertex.get(ii[2]));
                    gl.glNormal3fv(normal, 0);
                }
                for (int i : ii) {
                    gl.glVertex3f(vertex.get(i)[0], vertex.get(i)[1], vertex.get(i)[2]);
                }
            }
            gl.glEnd();
            gl.glDisable(GL2.GL_POLYGON_OFFSET_FILL);

            if (bb.isDrawOutline()) {
                rgba = bb.getOutlineColor().getRGBComponents(null);
                gl.glColor4f(rgba[0], rgba[1], rgba[2], rgba[3]);
                gl.glLineWidth(bb.getOutlineSize() * this.dpiScale);
                gl.glBegin(GL2.GL_LINES);
                for (int[] ii : cubic.getLineIndex()) {
                    for (int i : ii) {
                        gl.glVertex3f(vertex.get(i)[0], vertex.get(i)[1], vertex.get(i)[2]);
                    }
                }
                gl.glEnd();
            }
        }
    }

    void drawCylinder(GL2 gl, Graphic graphic) {
        boolean isDraw = true;
        if (this.clipPlane)
            isDraw = drawExtent.intersects(graphic.getExtent());

        if (isDraw) {
            CylinderShape cylinder = (CylinderShape) graphic.getShape();
            BarBreak bb = (BarBreak) graphic.getLegend();
            java.util.List<PointZ> ps = cylinder.getPoints();
            java.util.List<float[]> vertex = new ArrayList<>();
            for (PointZ p : ps) {
                vertex.add(new float[]{(float) p.X, (float) p.Y, (float) p.Z});
            }
            double height = vertex.get(1)[2] - vertex.get(0)[2];

            gl.glPushMatrix();
            gl.glPushAttrib(GL2.GL_POLYGON_BIT); // includes GL_CULL_FACE
            gl.glDisable(GL2.GL_CULL_FACE); // draw from all sides

            float[] rgba = bb.getColor().getRGBComponents(null);
            gl.glColor4f(rgba[0], rgba[1], rgba[2], rgba[3]);
            gl.glTranslatef(vertex.get(0)[0], vertex.get(0)[1], vertex.get(0)[2]);
            GLUquadric cone_obj = glu.gluNewQuadric();
            glu.gluCylinder(cone_obj, cylinder.getRadius(), cylinder.getRadius(), height, 100, 1);
            bb.setDrawOutline(false);
            this.drawCircle(gl, (float) height, (float) cylinder.getRadius(), bb);
            this.drawCircle(gl, 0.f, (float) cylinder.getRadius(), bb, true);

            gl.glPopAttrib(); // GL_CULL_FACE
            gl.glPopMatrix();
        }
    }

    void drawLegend(GL2 gl, ChartColorBar legend) {
        LegendScheme ls = legend.getLegendScheme();
        int bNum = ls.getBreakNum();
        if (ls.getLegendBreaks().get(bNum - 1).isNoData()) {
            bNum -= 1;
        }

        float x = 1.6f;
        x += legend.getXShift() * this.lenScale;
        float y = -1.0f;
        float lHeight = 2.0f;
        float lWidth = lHeight / legend.getAspect();
        java.util.List<Integer> labelIdxs = new ArrayList<>();
        List<String> tLabels = new ArrayList<>();
        if (legend.isAutoTick()) {
            float legendLen = this.toScreenLength(x, y, 0.0f, x, y + lHeight, 0.0f);
            int tickGap = this.getLegendTickGap(legend, legendLen);
            int sIdx = (bNum % tickGap) / 2;
            int labNum = bNum - 1;
            if (ls.getLegendType() == LegendType.UNIQUE_VALUE) {
                labNum += 1;
            } else if (legend.isDrawMinLabel()) {
                sIdx = 0;
                labNum = bNum;
            }
            while (sIdx < labNum) {
                labelIdxs.add(sIdx);
                sIdx += tickGap;
            }
        } else {
            int tickIdx;
            for (int i = 0; i < bNum; i++) {
                ColorBreak cb = ls.getLegendBreaks().get(i);
                double v = Double.parseDouble(cb.getEndValue().toString());
                if (legend.getTickLocations().contains(v)) {
                    labelIdxs.add(i);
                    tickIdx = legend.getTickLocations().indexOf(v);
                    tLabels.add(legend.getTickLabels().get(tickIdx).getText());
                }
            }
        }

        float barHeight = lHeight / bNum;

        //Draw color bar
        gl.glDepthFunc(GL.GL_ALWAYS);
        float yy = y;
        float[] rgba;
        for (int i = 0; i < bNum; i++) {
            //Fill color
            rgba = ls.getLegendBreak(i).getColor().getRGBComponents(null);
            gl.glColor4f(rgba[0], rgba[1], rgba[2], rgba[3]);
            gl.glBegin(GL2.GL_QUADS);
            gl.glVertex2f(x, yy);
            gl.glVertex2f(x + lWidth, yy);
            gl.glVertex2f(x + lWidth, yy + barHeight);
            gl.glVertex2f(x, yy + barHeight);
            gl.glEnd();
            yy += barHeight;
        }

        //Draw neatline
        rgba = legend.getTickColor().getRGBComponents(null);
        gl.glColor4f(rgba[0], rgba[1], rgba[2], rgba[3]);
        gl.glLineWidth(legend.getNeatLineSize() * this.dpiScale);
        gl.glBegin(GL2.GL_LINE_STRIP);
        gl.glVertex2f(x, y);
        gl.glVertex2f(x, y + lHeight);
        gl.glVertex2f(x + lWidth, y + lHeight);
        gl.glVertex2f(x + lWidth, y);
        gl.glVertex2f(x, y);
        gl.glEnd();

        //Draw ticks
        int idx = 0;
        yy = y;
        String caption;
        float tickLen = legend.getTickLength() * this.lenScale;
        float labelX = x + lWidth;
        if (legend.isInsideTick()) {
            if (tickLen > lWidth)
                tickLen = lWidth;
        } else {
            labelX += tickLen;
        }
        float strWidth = 0;
        Rectangle2D rect;
        float xShift = this.tickSpace * this.dpiScale;
        this.updateTextRender(legend.getTickLabelFont());
        for (int i = 0; i < bNum; i++) {
            if (labelIdxs.contains(i)) {
                ColorBreak cb = ls.getLegendBreaks().get(i);
                if (legend.isAutoTick()) {
                    if (ls.getLegendType() == LegendType.UNIQUE_VALUE) {
                        caption = cb.getCaption();
                    } else {
                        caption = DataConvert.removeTailingZeros(cb.getEndValue().toString());
                    }
                } else {
                    caption = tLabels.get(idx);
                }
                if (ls.getLegendType() == LegendType.UNIQUE_VALUE) {
                    rect = this.drawString(gl, caption, legend.getTickLabelFont(), legend.getTickLabelColor(),
                            x + lWidth, yy + barHeight * 0.5f, 0, XAlign.LEFT, YAlign.CENTER, xShift, 0);
                } else {
                    rgba = legend.getTickColor().getRGBComponents(null);
                    gl.glColor4f(rgba[0], rgba[1], rgba[2], rgba[3]);
                    gl.glLineWidth(legend.getTickWidth() * this.dpiScale);
                    gl.glBegin(GL2.GL_LINES);
                    if (legend.isInsideTick())
                        gl.glVertex2f(x + lWidth - tickLen, yy + barHeight);
                    else
                        gl.glVertex2f(x + lWidth + tickLen, yy + barHeight);
                    gl.glVertex2f(x + lWidth, yy + barHeight);
                    gl.glEnd();
                    rect = this.drawString(gl, caption, legend.getTickLabelFont(), legend.getTickLabelColor(),
                            labelX, yy + barHeight, 0, XAlign.LEFT, YAlign.CENTER, xShift, 0);
                }
                if (strWidth < rect.getWidth())
                    strWidth = (float) rect.getWidth();

                idx += 1;
            }
            yy += barHeight;
        }

        //Draw label
        ChartText label = legend.getLabel();
        if (label != null) {
            this.updateTextRender(legend.getLabelFont());
            label.setColor(legend.getTickColor());
            float sx, sy;
            float yShift = this.tickSpace * this.dpiScale;
            switch (legend.getLabelLocation()) {
                case "top":
                    sx = x + lWidth * 0.5f;
                    sy = y + lHeight;
                    drawString(gl, label, sx, sy, 0.0f, XAlign.CENTER, YAlign.BOTTOM, 0, 0, yShift);
                    break;
                case "bottom":
                    sx = x + lWidth * 0.5f;
                    sy = y;
                    yShift = -yShift;
                    drawString(gl, label, sx, sy, 0.0f, XAlign.CENTER, YAlign.TOP, 0, 0, yShift);
                    break;
                case "left":
                case "in":
                    sx = x;
                    sy = y + lHeight * 0.5f;
                    drawString(gl, label, sx, sy, 0.0f, XAlign.CENTER, YAlign.BOTTOM, 90.f, 0, yShift);
                    break;
                default:
                    sx = labelX;
                    sy = y + lHeight * 0.5f;
                    yShift = -strWidth - yShift;
                    drawString(gl, label, sx, sy, 0.0f, XAlign.CENTER, YAlign.TOP, 90.f, 0, yShift);
                    break;
            }
        }
        gl.glDepthFunc(GL2.GL_LEQUAL);
    }

    void drawColorbar(GL2 gl, ChartColorBar legend) {
        LegendScheme ls = legend.getLegendScheme();
        ColorMap colorMap = ls.getColorMap();
        Normalize normalize = ls.getNormalize();
        int bNum = colorMap.getColorCount();
        if (normalize instanceof BoundaryNorm) {
            bNum = ((BoundaryNorm) normalize).getNRegions();
        }

        float x = 1.6f;
        x += legend.getXShift() * this.lenScale;
        float y = -1.0f;
        float legendHeight = 2.0f;
        float barWidth = legendHeight / legend.getAspect();
        float minMaxHeight = legendHeight;
        float y_shift = 0;
        switch (legend.getExtendType()) {
            case MIN:
                minMaxHeight -= barWidth;
                y_shift += barWidth;
                break;
            case MAX:
                minMaxHeight -= barWidth;
                break;
            case BOTH:
                minMaxHeight -= barWidth * 2;
                y_shift += barWidth;
                break;
        }
        float barHeight = minMaxHeight / bNum;

        //Draw color bar
        gl.glDepthFunc(GL.GL_ALWAYS);
        float yy = y;
        float[] rgba;
        Color[] colors = colorMap.getColors(bNum);
        for (int i = 0; i < bNum; i++) {
            //Fill color
            rgba = colors[i].getRGBComponents(null);
            gl.glColor4f(rgba[0], rgba[1], rgba[2], rgba[3]);
            gl.glBegin(GL2.GL_QUADS);
            gl.glVertex2f(x, yy);
            gl.glVertex2f(x + barWidth, yy);
            gl.glVertex2f(x + barWidth, yy + barHeight);
            gl.glVertex2f(x, yy + barHeight);
            gl.glEnd();
            yy += barHeight;
        }

        //Draw neatline
        rgba = legend.getTickColor().getRGBComponents(null);
        gl.glColor4f(rgba[0], rgba[1], rgba[2], rgba[3]);
        gl.glLineWidth(legend.getNeatLineSize() * this.dpiScale);
        gl.glBegin(GL2.GL_LINE_STRIP);
        gl.glVertex2f(x, y);
        gl.glVertex2f(x, y + legendHeight);
        gl.glVertex2f(x + barWidth, y + legendHeight);
        gl.glVertex2f(x + barWidth, y);
        gl.glVertex2f(x, y);
        gl.glEnd();

        //Draw ticks
        int idx = 0;
        yy = y;
        String caption;
        float tickLen = legend.getTickLength() * this.lenScale;
        float labelX = x + barWidth;
        if (legend.isInsideTick()) {
            if (tickLen > barWidth)
                tickLen = barWidth;
        } else {
            labelX += tickLen;
        }
        float strWidth = 0;
        Rectangle2D rect;
        float xShift = this.tickSpace * this.dpiScale;
        this.updateTextRender(legend.getTickLabelFont());
        for (int i = 0; i < legend.getTickLocations().size(); i++) {
            yy = y + minMaxHeight * normalize.apply(legend.getTickLocations().get(i)).floatValue();
            String label = legend.getTickLabels().get(i).getText();
            rgba = legend.getTickColor().getRGBComponents(null);
            gl.glColor4f(rgba[0], rgba[1], rgba[2], rgba[3]);
            gl.glLineWidth(legend.getTickWidth() * this.dpiScale);
            gl.glBegin(GL2.GL_LINES);
            if (legend.isInsideTick())
                gl.glVertex2f(x + barWidth - tickLen, yy);
            else
                gl.glVertex2f(x + barWidth + tickLen, yy);
            gl.glVertex2f(x + barWidth, yy);
            gl.glEnd();
            rect = this.drawString(gl, label, legend.getTickLabelFont(), legend.getTickLabelColor(),
                    labelX, yy, 0, XAlign.LEFT, YAlign.CENTER, xShift, 0);
            if (strWidth < rect.getWidth())
                strWidth = (float) rect.getWidth();
        }

        //Draw label
        ChartText label = legend.getLabel();
        if (label != null) {
            this.updateTextRender(legend.getLabelFont());
            label.setColor(legend.getTickColor());
            float sx, sy;
            float yShift = this.tickSpace * this.dpiScale;
            switch (legend.getLabelLocation()) {
                case "top":
                    sx = x + barWidth * 0.5f;
                    sy = y + legendHeight;
                    drawString(gl, label, sx, sy, 0.0f, XAlign.CENTER, YAlign.BOTTOM, 0, 0, yShift);
                    break;
                case "bottom":
                    sx = x + barWidth * 0.5f;
                    sy = y;
                    yShift = -yShift;
                    drawString(gl, label, sx, sy, 0.0f, XAlign.CENTER, YAlign.TOP, 0, 0, yShift);
                    break;
                case "left":
                case "in":
                    sx = x;
                    sy = y + legendHeight * 0.5f;
                    drawString(gl, label, sx, sy, 0.0f, XAlign.CENTER, YAlign.BOTTOM, 90.f, 0, yShift);
                    break;
                default:
                    sx = labelX;
                    sy = y + legendHeight * 0.5f;
                    yShift = -strWidth - yShift;
                    drawString(gl, label, sx, sy, 0.0f, XAlign.CENTER, YAlign.TOP, 90.f, 0, yShift);
                    break;
            }
        }
        gl.glDepthFunc(GL2.GL_LEQUAL);
    }

    /**
     * Get legend scheme
     *
     * @return Legend scheme
     */
    public LegendScheme getLegendScheme() {
        LegendScheme ls = null;
        int n = this.graphics.getNumGraphics();
        for (int i = n - 1; i >= 0; i--) {
            Graphic g = this.graphics.getGraphicN(i);
            if (g instanceof GraphicCollection) {
                ls = ((GraphicCollection)g).getLegendScheme();
                break;
            }
        }

        if (ls == null) {
            ShapeTypes stype = ShapeTypes.POLYLINE;
            ls = new LegendScheme(stype);
            for (Graphic g : this.graphics.getGraphics()) {
                ls.getLegendBreaks().add(g.getLegend());
            }
        }
        return ls;
    }

    /**
     * Get extent scale - extent / draw extent
     * @return Extent scale
     */
    public float getScale() {
        return (float) (this.graphicExtent.getWidth() / this.drawExtent.getWidth());
    }

    public void dispose(GLAutoDrawable drawable) {
        GL2 gl = drawable.getGL().getGL2();
        Program.destroyAllPrograms(gl);
        this.alwaysUpdateBuffers = true;
    }

    public void init(GLAutoDrawable drawable) {
        this.drawable = drawable;
        drawable.getGL().setSwapInterval(1);
        //drawable.getContext().makeCurrent();
        GL2 gl = drawable.getGL().getGL2();
        this.gl = gl;
        this.glu = GLU.createGLU(gl);
        Font font = this.xAxis.getTickLabelFont();
        updateTextRender(font);
        //Background
        //gl.glClearColor(1f, 1f, 1f, 1.0f);
        gl.glEnable(GL2.GL_POINT_SMOOTH);
        gl.glEnable(GL2.GL_DEPTH_TEST);
        gl.glShadeModel(GL2.GL_SMOOTH);
        //gl.glShadeModel(GL2.GL_FLAT);
        gl.glDepthFunc(GL2.GL_LEQUAL);
        //gl.glDepthFunc(GL2.GL_LESS);
        gl.glHint(GL2.GL_PERSPECTIVE_CORRECTION_HINT, GL2.GL_NICEST);

        //jogl specific addition for tessellation
        tessCallback = new Plot3DGL.TessCallback(gl, glu);

        this.positionArea = new Rectangle2D.Double(0, 0, 1, 1);
    }

    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
        //this.width = width;
        //this.height = height;
        this.positionArea = this.getPositionArea(new Rectangle2D.Double(0, 0, width, height));
        this.setGraphArea(this.positionArea);
        this.width = (int) this.positionArea.getWidth();
        this.height = (int) this.positionArea.getHeight();

        final GL2 gl = drawable.getGL().getGL2();
        if (height <= 0) {
            height = 1;
        }

        final float h = (float) width / (float) height;
        gl.glViewport((int) positionArea.getX(), (int) (height - this.height - positionArea.getY()),
                this.width, this.height);
        gl.glMatrixMode(GL2.GL_PROJECTION);
        gl.glLoadIdentity();

        if (this.orthographic) {
            float v = 2.0f;
            switch (this.aspectType) {
                case EQUAL:
                    gl.glOrthof(-v * h, v * h, -v, v, -distance, distance);
                    break;
                default:
                    gl.glOrthof(-v, v, -v, v, -distance, distance);
                    break;
            }
        } else {
            float near = 0.1f;
            float far = 1000.0f;
            switch (this.aspectType) {
                case EQUAL:
                    glu.gluPerspective(fieldOfView, h, near, far);
                    break;
                default:
                    glu.gluPerspective(fieldOfView, 1.0f, near, far);
                    break;
            }
            float z = distance * 45.f / fieldOfView;
            glu.gluLookAt(0.0f, 0.0f, z, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f);
        }
        gl.glMatrixMode(GL2.GL_MODELVIEW);
        gl.glLoadIdentity();
    }

    /**
     * Update projections
     */
    public void updateProjections(GLAutoDrawable drawable) {
        final GL2 gl = drawable.getGL().getGL2();

        final float h = (float) width / (float) height;
        gl.glMatrixMode(GL2.GL_PROJECTION);
        gl.glLoadIdentity();

        if (this.orthographic) {
            float v = 2.0f;
            switch (this.aspectType) {
                case EQUAL:
                    gl.glOrthof(-v * h, v * h, -v, v, -distance, distance);
                    break;
                default:
                    gl.glOrthof(-v, v, -v, v, -distance, distance);
                    break;
            }
        } else {
            float near = 0.1f;
            float far = 1000.0f;
            switch (this.aspectType) {
                case EQUAL:
                    glu.gluPerspective(45.0f, h, near, far);
                    break;
                default:
                    glu.gluPerspective(45.0f, 1.0f, near, far);
                    break;
            }
            glu.gluLookAt(0.0f, 0.0f, distance, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f);
        }
        gl.glMatrixMode(GL2.GL_MODELVIEW);
        gl.glLoadIdentity();
    }

    /**
     * Get tight inset area
     *
     * @param g Graphics2D
     * @param positionArea Position area
     * @return Tight inset area
     */
    @Override
    public Margin getTightInset(Graphics2D g, Rectangle2D positionArea) {
        int left = 2, bottom = 2, right = 2, top = 5;
        int space = 2;

        if (this.title != null) {
            top += this.title.getTrueDimension(g).height + 10;
        }

        if (!this.legends.isEmpty()) {
            ChartLegend legend = this.getLegend();
            Dimension dim = legend.getLegendDimension(g, new Dimension((int) positionArea.getWidth(),
                    (int) positionArea.getHeight()));
            switch (legend.getPosition()) {
                case UPPER_CENTER_OUTSIDE:
                    top += dim.height + 10;
                    break;
                case LOWER_CENTER_OUTSIDE:
                    bottom += dim.height + legend.getYShift() + 10;
                    break;
                case LEFT_OUTSIDE:
                    left += dim.width + 10;
                    break;
                case RIGHT_OUTSIDE:
                    right += dim.width + legend.getXShift() + 10;
                    break;
            }
        }

        return new Margin(left, right, top, bottom);
    }

    /**
     * Clone
     * @return Cloned Plot3DGL
     */
    public Object clone() {
        Plot3DGL plot3DGL = new Plot3DGL();
        plot3DGL.graphics = this.graphics;
        plot3DGL.angleX = this.angleX;
        plot3DGL.angleY = this.angleY;
        plot3DGL.background = this.background;
        plot3DGL.sampleBuffers = this.sampleBuffers;
        plot3DGL.antialias = this.antialias;
        plot3DGL.boxColor = this.boxColor;
        plot3DGL.boxed = this.boxed;
        plot3DGL.clipPlane = this.clipPlane;
        plot3DGL.displayXY = this.displayXY;
        plot3DGL.displayZ = this.displayZ;
        plot3DGL.dpiScale = this.dpiScale;
        plot3DGL.drawBase = this.drawBase;
        plot3DGL.drawBoundingBox = this.drawBoundingBox;
        plot3DGL.setDrawExtent((Extent3D) this.drawExtent.clone());
        plot3DGL.gridLine = this.gridLine;
        plot3DGL.legends = this.legends;
        plot3DGL.hideOnDrag = this.hideOnDrag;
        plot3DGL.lighting = this.lighting;
        plot3DGL.title = this.title;

        return plot3DGL;
    }

    public static void main(String[] args) {

        final GLProfile gp = GLProfile.get(GLProfile.GL2);
        GLCapabilities cap = new GLCapabilities(gp);

        final GLChartPanel gc = new GLChartPanel(cap, new Plot3DGL());
        gc.setSize(400, 400);

        final JFrame frame = new JFrame("JOGL Line");
        frame.add(gc);
        frame.setSize(500, 400);
        frame.setVisible(true);

        //gc.animator_start();
    }

    // </editor-fold>

    public static class TessCallback extends com.jogamp.opengl.glu.GLUtessellatorCallbackAdapter {
        GL2 gl;
        GLU glu;

        public TessCallback(GL2 gl, GLU glu) {
            this.gl = gl;
            this.glu = glu;
        };
        public void begin(int type) {
            gl.glBegin(type);
        }

        public void end() {
            gl.glEnd();
        }

        public void vertex(Object data) {
            if (data instanceof double[]) {
                double[] d = (double[]) data;
                if (d.length == 6) {
                    gl.glColor3dv(d, 3);
                }
                gl.glVertex3dv(d, 0);
            }
        }

        public void error(int errnum) {
            String estring;
            estring = glu.gluErrorString(errnum);
            System.out.println("Tessellation Error: " + estring);
            //System.exit(0);
            throw new RuntimeException();
        }

        public void combine(double[] coords, Object[] data,
                            float[] weight, Object[] outData) {
            double[] vertex = new double[6];

            int i;
            vertex[0] = coords[0];
            vertex[1] = coords[1];
            vertex[2] = coords[2];
            for (i = 3; i < 6; i++)
                vertex[i] = weight[0] * ((double[]) data[0])[i] +
                        weight[1] * ((double[]) data[1])[i] +
                        weight[2] * ((double[]) data[2])[i] +
                        weight[3] * ((double[]) data[3])[i];
            outData[0] = vertex;
        }
    }//End TessCallback
}
