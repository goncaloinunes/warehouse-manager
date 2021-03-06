package ggc.core;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Recipe implements Serializable {
    private double _alpha;
    private AggregateProduct _product;
    private List<Component> _components = new ArrayList<Component>();

    Recipe(AggregateProduct product, List<Component> components, double alpha) {
        _alpha = alpha;
        _components = components;
        _product = product;
    }

    double getAlpha() {
        return _alpha;
    }

    List<Component> getComponents() {
        return _components;
    }

    public String toString() {
        String ret = "";

        for(Component component: _components) {
            ret += component.toString() + "#";
        }

        return ret.substring(0, ret.length()-1);
    }
}
