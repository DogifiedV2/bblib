package com.ruben.bblib.api.molang;

import com.ruben.bblib.api.model.data.Vec3f;

public class MolangVec3 {

    private final MolangValue x;
    private final MolangValue y;
    private final MolangValue z;
    private final boolean hasMolang;

    public MolangVec3(float x, float y, float z) {
        this.x = ctx -> x;
        this.y = ctx -> y;
        this.z = ctx -> z;
        this.hasMolang = false;
    }

    public MolangVec3(String xExpr, String yExpr, String zExpr) {
        boolean xIsMolang = MolangParser.isMolangExpression(xExpr);
        boolean yIsMolang = MolangParser.isMolangExpression(yExpr);
        boolean zIsMolang = MolangParser.isMolangExpression(zExpr);

        this.hasMolang = xIsMolang || yIsMolang || zIsMolang;

        if (xIsMolang) {
            this.x = MolangParser.parse(xExpr);
        } else {
            float xVal = parseFloat(xExpr);
            this.x = ctx -> xVal;
        }

        if (yIsMolang) {
            this.y = MolangParser.parse(yExpr);
        } else {
            float yVal = parseFloat(yExpr);
            this.y = ctx -> yVal;
        }

        if (zIsMolang) {
            this.z = MolangParser.parse(zExpr);
        } else {
            float zVal = parseFloat(zExpr);
            this.z = ctx -> zVal;
        }
    }

    private static float parseFloat(String str) {
        if (str == null || str.isBlank()) {
            return 0;
        }
        try {
            return Float.parseFloat(str.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public Vec3f evaluate(MolangContext context) {
        return new Vec3f(
                (float) x.evaluate(context),
                (float) y.evaluate(context),
                (float) z.evaluate(context)
        );
    }

    public boolean hasMolang() {
        return hasMolang;
    }

    public static MolangVec3 fromVec3f(Vec3f vec) {
        return new MolangVec3(vec.x(), vec.y(), vec.z());
    }

    public static MolangVec3 zero() {
        return new MolangVec3(0, 0, 0);
    }

    public static MolangVec3 one() {
        return new MolangVec3(1, 1, 1);
    }
}

