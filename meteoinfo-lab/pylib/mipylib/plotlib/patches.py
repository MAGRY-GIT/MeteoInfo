from org.meteoinfo.geometry.graphic import Graphic
from org.meteoinfo.geometry.shape import ShapeUtil, CircleShape, EllipseShape, \
    RectangleShape, ArcShape

from . import plotutil
import mipylib.numeric as np

__all__ = ['Arc','Circle','Ellipse','Rectangle','Polygon']

class Circle(Graphic):
    """
    A circle patch.
    """

    def __init__(self, xy, radius=5, **kwargs):
        """
        Create a true circle at center *xy* = (*x*, *y*) with given *radius*.

        :param xy: (float, float) xy coordinates of circle centre.
        :param radius: (float) Circle radius.
        """
        self._center = xy
        self._radius = radius
        shape = CircleShape(xy[0], xy[1], radius)
        legend, isunique = plotutil.getlegendbreak('polygon', **kwargs)
        super(Circle, self).__init__(shape, legend)

    @property
    def center(self):
        return self._center

    @property
    def radius(self):
        return self._radius

class Ellipse(Graphic):
    """
    A ellipse patch.
    """

    def __init__(self, xy, width, height, angle=0, **kwargs):
        """
        Create an ellipse at center *xy* = (*x*, *y*) with given *width* and *height*.

        :param xy: (float, float) xy coordinates of ellipse centre.
        :param width: (float) Ellipse width.
        :param height: (float) Ellipse height.
        :param angle: (float) Ellipse angle. Default is 0.
        """
        self._center = xy
        self._width = width
        self._height = height
        self._angle = angle
        shape = EllipseShape(xy[0], xy[1], width, height)
        shape.setAngle(angle)
        legend, isunique = plotutil.getlegendbreak('polygon', **kwargs)
        super(Ellipse, self).__init__(shape, legend)

    @property
    def center(self):
        return self._center

    @property
    def width(self):
        return self._width

    @property
    def height(self):
        return self._height

    @property
    def angle(self):
        return self._angle

class Arc(Graphic):
    """
    An Arc patch.
    """

    def __init__(self, xy, width, height, angle=0, theta1=0.0, theta2=360.0, **kwargs):
        """
        Create an arc at anchor point *xy* = (*x*, *y*) with given *width* and *height*.

        :param xy: (float, float) xy coordinates of anchor point.
        :param width: (float) Ellipse width.
        :param height: (float) Ellipse height.
        :param angle: (float) Ellipse angle. Default is `0`.
        :param theta1: (float) Starting angle of the arc in degrees. Default is `0.0`.
        :param theta2: (float) Ending angle of the arc in degrees. Default is `360`.
        """
        self._center = xy
        self._width = width
        self._height = height
        self._angle = angle
        self._theta1 = theta1
        self._theta2 = theta2
        shape = ArcShape(xy[0], xy[1], width, height)
        shape.setAngle(angle)
        shape.setStartAngle(theta1)
        shape.setSweepAngle(theta2 - theta1)
        legend, isunique = plotutil.getlegendbreak('polygon', **kwargs)
        super(Arc, self).__init__(shape, legend)

    @property
    def center(self):
        return self._center

    @property
    def width(self):
        return self._width

    @property
    def height(self):
        return self._height

    @property
    def angle(self):
        return self._angle

    @property
    def theta1(self):
        return self._theta1

    @property
    def theta2(self):
        return self._theta2

class Rectangle(Graphic):
    """
    A rectangle patch.
    """

    def __init__(self, xy, width, height, angle=0, **kwargs):
        """
        Create a rectangle at anchor point *xy* = (*x*, *y*) with given *width* and *height*.

        :param xy: (float, float) xy coordinates of anchor point.
        :param width: (float) Rectangle width.
        :param height: (float) Rectangle height.
        :param angle: (float) Rectangle angle. Default is 0.
        """
        self._center = xy
        self._width = width
        self._height = height
        self._angle = angle
        shape = RectangleShape(xy[0], xy[1], width, height)
        shape.setAngle(angle)
        legend, isunique = plotutil.getlegendbreak('polygon', **kwargs)
        super(Rectangle, self).__init__(shape, legend)

    @property
    def center(self):
        return self._center

    @property
    def width(self):
        return self._width

    @property
    def height(self):
        return self._height

    @property
    def angle(self):
        return self._angle

class Polygon(Graphic):
    """
    A general polygon patch.
    """

    def __init__(self, xy, closed=True, **kwargs):
        """
        Create a polygon with *xy* point array.

        :param xy: (array_like) xy point array.
        :param closed: (bool) If *closed* is *True*, the polygon will be closed so the
            starting and ending points are the same.
        """
        if isinstance(xy, (list, tuple)):
            xy = np.array(xy)

        self._xy = xy
        self._closed = closed
        shape = ShapeUtil.createPolygonShape(xy._array)
        legend, isunique = plotutil.getlegendbreak('polygon', **kwargs)
        super(Polygon, self).__init__(shape, legend)

    @property
    def xy(self):
        return self._xy