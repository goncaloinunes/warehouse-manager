package ggc.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.io.Serializable;
import java.lang.Math;


public class Partner implements Serializable {
    private String _name;
    private String _address;
    private String _id;
    private String _status;
    private double _points;
    private double _salesValue;
    private double _salesPayed;
    private ArrayList<Acquisition> _acquisitions = new ArrayList<Acquisition>();
    private ArrayList<Sale> _sales = new ArrayList<Sale>();
    private ArrayList<Batch> _batches = new ArrayList<Batch>();
    private ArrayList<Notification> _notifications = new ArrayList<Notification>();

    Partner(String id, String name, String address) {
        _id = id;
        _name = name;
        _address = address;
        _status = "NORMAL";
        _points = 0;
    }

    public String getId() {
        return _id;
    }

    public void clearNotifications() {
        _notifications.clear();
    }

    public List<Notification> getNotifications() {
        return Collections.unmodifiableList(_notifications);
    }

    public double calculateAcquisitionsValue() {
        Double total = 0d;

        for (Acquisition acquisition : _acquisitions) {
            total += acquisition.getBaseValue();
        }

        return total;
    }

    public double calculateSalesValue() {
        Double total = 0d;

        for (Sale sale : _sales) {
            if(sale instanceof SaleByCredit) {
                total += ((SaleByCredit)sale).getAmountPaid();
            }
        }

        return total;
    }

    public double calculateSalesPayedValue() {
        Double total = 0d;

        for (Sale sale : _sales) {
            if(sale.isPaid() && sale instanceof SaleByCredit) {
                total += ((SaleByCredit)sale).getAmountPaid();
            }
        }

        return total;
    }


    public String toString() {
        return _id + "|" + _name + "|" + _address + "|" +_status + "|" + Math.round(_points) + "|" 
        + Math.round(calculateAcquisitionsValue()) + "|" + Math.round(calculateSalesValue()) + "|" + Math.round(calculateSalesPayedValue());
    }
}
