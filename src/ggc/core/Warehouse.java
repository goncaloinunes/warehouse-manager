package ggc.core;


import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.io.IOException;

import ggc.app.exception.UnavailableProductException;
import ggc.core.exception.BadEntryException;
import ggc.core.exception.DuplicatePartnerException;
import ggc.core.exception.InvalidDaysException;
import ggc.core.exception.UnavailableProductQuantityException;
import ggc.core.exception.UnknownPartnerException;
import ggc.core.exception.UnknownProductException;
import ggc.core.exception.UnknownTransactionException;


/**
 * Class Warehouse implements a warehouse.
 */
public class Warehouse implements Serializable {

    /** Serial number for serialization. */
    private static final long serialVersionUID = 202109192006L;


    private Date _date;
    private int _nextTransactionId;
    private double _availableBalance;
    private Map<String, Product> _products = new TreeMap<String, Product>(String.CASE_INSENSITIVE_ORDER);
    private Map<String, Partner> _partners = new TreeMap<String, Partner>(String.CASE_INSENSITIVE_ORDER);
    private Map<Integer, Transaction> _transactions = new TreeMap<Integer, Transaction>();

    Warehouse() {
        _date = new Date();
        _nextTransactionId = 0;
        _availableBalance = 0;
    }

    Date getDate() {
        return _date;
    }

    void advanceDate(int offset) throws InvalidDaysException {
        if (offset <= 0) {
            throw new InvalidDaysException(offset);
        }

        _date.add(offset);

        for(Transaction transaction : _transactions.values()) {
            transaction.setCurrentDate(_date);
        }
    }

    Collection<Product> getProducts() {
        return Collections.unmodifiableCollection(_products.values());
    }

    Collection<Partner> getPartners() {
        return Collections.unmodifiableCollection(_partners.values());
    }

    Collection<Transaction> getTransactions() {
        return Collections.unmodifiableCollection(_transactions.values());
    }

    List<Batch> getAllBatchesSorted() {
        ArrayList<Batch> batches = new ArrayList<Batch>();

        for(Product product : getProducts()) {
          for(Batch batch : product.getBatches()) {
            batches.add(batch);
          }
        }
    
        batches.sort(new BatchComparator());
    
        return Collections.unmodifiableList(batches);
    }


    /**
     * @param txtfile filename to be loaded.
     * @throws IOException
     * @throws BadEntryException
     * @throws UnknownPartnerException
     * @throws UnknownProductException
     * @throws NumberFormatException
     */
    void importFile(String txtfile) throws IOException, BadEntryException, DuplicatePartnerException, UnknownPartnerException, NumberFormatException, UnknownProductException {
        Parser parser = new Parser(this);
        parser.parseFile(txtfile);
    }

    void registerPartner(String id, String name, String address) throws DuplicatePartnerException {
        if(_partners.containsKey(id)) {
            throw new DuplicatePartnerException(id);
        }
        Partner partner =  new Partner(id, name, address);

        for(Product product : _products.values()) {
            product.registerObserver(partner);
        }
        _partners.put(id, partner);
    }

    void makeProductOberversInterested(Product product) {
        for (ProductObserver observer : _partners.values()) {
            product.registerObserver(observer);
        }
    }

    void registerSimpleProduct(String productId) {
        SimpleProduct product = new SimpleProduct(productId);
        makeProductOberversInterested(product);
        _products.put(productId, product);
    }

    void registerAggregateProduct(String productId, List<Product> products, List<Integer> quantities, double alpha) {

        List<Component> components = new ArrayList<Component>();
        AggregateProduct aggregateProduct = new AggregateProduct(productId);

        for(int i = 0; i < products.size(); i++) {
            components.add(new Component(quantities.get(i), products.get(i)));
        }

        aggregateProduct.setRecipe(new Recipe(aggregateProduct, components, alpha));
        makeProductOberversInterested(aggregateProduct);
        _products.put(productId, aggregateProduct);
    }

    Product getProductWithId(String id) throws UnknownProductException {
        if(!productExists(id)) {
            throw new UnknownProductException(id);
        }
        return _products.get(id);
    }

    Collection<Batch> getBatchesFromProduct(String id) throws UnknownProductException {
        return getProductWithId(id).getBatches();
    }

    Collection<Batch> getBatchesFromPartner(String id) throws UnknownPartnerException {
        return getPartnerWithId(id).getBatches();
    }

    List<Batch> getBatchesUnderGivenPrice(int price) {
        List<Batch> lookup = new ArrayList<Batch>();
        for(Product product : getProducts()) {
          for(Batch batch : product.getBatches()) {
            if (price > batch.getPrice())
                lookup.add(batch);
          }
        }
        lookup.sort(new BatchComparator());
        return Collections.unmodifiableList(lookup);

    }

    Partner getPartnerWithId(String id) throws UnknownPartnerException {
        if(!partnerExists(id)) {
            throw new UnknownPartnerException(id);
        }

        return _partners.get(id);
    }

    boolean partnerExists(String id) {
        return _partners.containsKey(id);
    }

    boolean productExists(String id) {
        return _products.containsKey(id);
    }

    Collection<Acquisition> getAcquisitionsFromPartner(String id) throws UnknownPartnerException {
        return getPartnerWithId(id).getAcquisitions();
    }

    Collection<Sale> getSalesFromPartner(String id) throws UnknownPartnerException {
        return getPartnerWithId(id).getSales();
    }

    Transaction getTransactionWithId(int id) throws UnknownTransactionException {
        if(!_transactions.containsKey(id)) {
            throw new UnknownTransactionException(id);
        }
        
        return _transactions.get(id);
    }

    void toggleNotifications(Product product, ProductObserver observer) {
        if(product.observerExists(observer)) {
            product.removeObserver(observer);
        } else {
            product.registerObserver(observer);
        }
      }


    List<Transaction> getPaymentsPartner(String id) throws UnknownPartnerException {
        ArrayList<Transaction> payments = new ArrayList<>();
        Partner partner = getPartnerWithId(id);
        
        for(Transaction transaction : getTransactions()) {
            if(transaction.isPaid() && transaction.getPartner().equals(partner))
                payments.add(transaction);
        }
        
        return Collections.unmodifiableList(payments);
    }

    void registerBreakdownTransaction(Partner partner, Product product, int amount) throws UnavailableProductQuantityException {

        if(product.getRecipe() != null) {
            for(Component component : product.getRecipe().getComponents()) {
                if(component.getProduct().getTotalStock() < amount) {
                    throw new UnavailableProductQuantityException(component.getProduct().getId(), amount, component.getProduct().getTotalStock());
                }
            }
        }

        if(amount > product.getTotalStock()) {
            throw new UnavailableProductQuantityException(product.getId(), amount, product.getTotalStock());
        }
        
        if(product.getRecipe() == null) {
            return;
        }

        int copyAmount = amount;
        List<Batch> batches = new LinkedList<Batch>(product.getBatches());
        batches.sort(new Comparator<Batch>() {
            public int compare(Batch b1, Batch b2) {
                return (int)(b1.getPrice() - b2.getPrice());
            }
        });

        double price;
        double acquisitions = 0;
        double sales = 0;
        List<Batch> newBatches = new ArrayList<Batch>();
        while(amount > 0) {
            Batch batch = batches.get(0);
            Recipe recipe = batch.getProduct().getRecipe();
            if(batch.getQuantity() > amount) { 
                for(Component component : recipe.getComponents()) {
                    if(component.getProduct().getBatches().size() == 0) {
                        price = component.getProduct().getAllTimeHigh();
                    } else {
                        price = component.getProduct().getMinPrice();
                    }

                    acquisitions += price * amount * component.getQuantity();
                    component.getProduct().addBatch(price, amount * component.getQuantity(), partner);
                    newBatches.add(new Batch(price * amount * component.getQuantity(), component.getQuantity() * amount, component.getProduct()));
                }
                sales += batch.getPrice() * amount;
                batch.removeQuantity(amount);
                amount = 0;
            } else {
                for(Component component : recipe.getComponents()) {
                    if(component.getProduct().getBatches().size() == 0) {
                        price = component.getProduct().getAllTimeHigh();
                    } else {
                        price = component.getProduct().getMinPrice();
                    }
                    acquisitions += price * batch.getQuantity() * component.getQuantity();
                    component.getProduct().addBatch(price, batch.getQuantity() * component.getQuantity(), partner);
                    newBatches.add(new Batch(price * batch.getQuantity() * component.getQuantity(), component.getQuantity() * batch.getQuantity(), component.getProduct()));
                }
                batches.remove(batch);
                product.removeBatch(batch);
                sales += batch.getPrice() * batch.getQuantity();
                amount -= batch.getQuantity();
            }
        }

        double baseValue = sales - acquisitions;
        BreakdownSale transaction = new BreakdownSale(_nextTransactionId, product, copyAmount, partner);
        transaction.setBatches(newBatches);
        transaction.setBaseValue(baseValue);
        transaction.setCurrentDate(new Date(_date.getDays()));
        transaction.setPaymentDate(new Date(_date.getDays()));
        _transactions.put(_nextTransactionId, transaction);
        partner.addSale(transaction);
        _nextTransactionId++;
        _availableBalance += transaction.getAmountPaid();
    }

    void registerAcquisitionTransaction(Partner partner, Product product, double price, int quantity) {
        Acquisition acquisition = new Acquisition(_nextTransactionId, product, quantity, partner, price);
        acquisition.setCurrentDate(new Date(_date.getDays()));
        acquisition.setPaymentDate(new Date(_date.getDays()));
        _transactions.put(_nextTransactionId, acquisition);

        partner.addAcquisition(acquisition);
        product.addBatch(price, quantity, partner);

        _nextTransactionId++;
        _availableBalance -= price * quantity;
    }

    void pay(Transaction transaction) {
        if (transaction.isPaid()) {
            return;
        }

        transaction.pay();
        _availableBalance += transaction.getAmountPaid();
    }

    public void registerSaleTransaction(Partner partner, Product product, int deadline, int amount) throws UnavailableProductQuantityException {
        SaleByCredit sale = new SaleByCredit(_nextTransactionId, product, amount, partner, deadline);

        if(amount > product.getTotalStock()) {
            throw new UnavailableProductQuantityException(product.getId(), amount, product.getTotalStock());
        }

        List<Batch> batches = new ArrayList<Batch>(product.getBatches());
        batches.sort(new Comparator<Batch>() {
            public int compare(Batch b1, Batch b2) {
                return (int)(b1.getPrice() - b2.getPrice());
            }
        });

        int price = 0;
        for(Batch batch : batches) {
            if(batch.getQuantity() > amount) {
                price += amount * batch.getPrice();
                batch.removeQuantity(amount);
                break;
            } else {
                price += batch.getQuantity() * batch.getPrice();
                product.removeBatch(batch);
                amount -= batch.getQuantity();
            }
        }

        sale.setCurrentDate(new Date(_date.getDays()));
        sale.setBaseValue(price);
        _transactions.put(_nextTransactionId, sale);
        _nextTransactionId++;
        partner.addSale(sale);
    }

    double getAvailableBalance() {
        return _availableBalance;
    }

    double getAccountingBalance() {
        double accountingBalance = _availableBalance;

        for(Transaction transaction : _transactions.values()) {
            if(!transaction.isPaid()) {
                accountingBalance += transaction.getTotalValue();
            }
        }

        return accountingBalance;
    }
    
}
