public class CelestialCoordinate {
    private float ra;
    private float dec;

    public CelestialCoordinate(float ra, float dec) {
        this.ra = ra;
        this.dec = dec;
    }

    public void setRightAcension(float ra) {
        this.ra = ra;
    }

    public float getRightAcension() {
        return ra;
    }

    public void setDeclination(float dec) {
        this.dec = dec;
    }

    public float getDeclination() {
        return dec;
    }
}