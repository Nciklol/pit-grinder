package boats.jojo.grindbot.structs;

public class APIVec3 {
    private double x;
    private double y;
    private double z;

    public APIVec3() {}
    public APIVec3(double _x, double _y, double _z) {
        x = _x;
        y = _y;
        z = _z;
    }

    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }

    public double getZ() {
        return z;
    }

    public void setZ(double z) {
        this.z = z;
    }
}
