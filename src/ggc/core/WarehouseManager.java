package ggc.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

import ggc.app.exception.UnavailableProductException;
import ggc.core.exception.BadEntryException;
import ggc.core.exception.DuplicatePartnerException;
import ggc.core.exception.ImportFileException;
import ggc.core.exception.InvalidDaysException;
import ggc.core.exception.UnavailableFileException;
import ggc.core.exception.UnavailableProductQuantityException;
import ggc.core.exception.UnknownPartnerException;
import ggc.core.exception.UnknownProductException;
import ggc.core.exception.UnknownTransactionException;
import ggc.core.exception.MissingFileAssociationException;

/** Façade for access. */
public class WarehouseManager {

  /** Name of file storing current warehouse. */
  private String _filename = "";

  /** The wharehouse itself. */
  private Warehouse _warehouse = new Warehouse();


  /**
   * @@throws IOException
   * @@throws FileNotFoundException
   * @@throws MissingFileAssociationException
   * @throws ClassNotFoundException
   */
  public void save() throws IOException, FileNotFoundException, MissingFileAssociationException {
    try (ObjectOutputStream obOut = new ObjectOutputStream(new FileOutputStream(_filename))) {
      obOut.writeObject(_warehouse);
    }
  }

  /**
   * @@param filename
   * @@throws MissingFileAssociationException
   * @@throws IOException
   * @@throws FileNotFoundException
   * @throws ClassNotFoundException
   */
  public void saveAs(String filename) throws IOException, FileNotFoundException, MissingFileAssociationException {
    _filename = filename;
    save();
  }

  /**
   * @@param filename
   * @@throws UnavailableFileException
   * @throws FileNotFoundException
   * @throws IOException
   */
  public void load(String filename) throws UnavailableFileException, ClassNotFoundException {
   
    try (ObjectInputStream objIn = new ObjectInputStream(new FileInputStream(filename))) {

      _warehouse = (Warehouse)objIn.readObject();
      _filename = filename;
    
    } catch (IOException e) {
      throw new UnavailableFileException(filename);
    }
  }

  /**
   * @param textfile
   * @throws ImportFileException
   */
  public void importFile(String textfile) throws ImportFileException {
    try {
      _warehouse.importFile(textfile);
    } catch (IOException | BadEntryException | DuplicatePartnerException | UnknownPartnerException | UnknownProductException e) {
      throw new ImportFileException(textfile, e);
    }
  }

  public boolean isFilenameSet() {
    return !_filename.isEmpty();
  }

  public int getDate() {
    return _warehouse.getDate().getDays();
  }

  public void advanceDate(int offset) throws InvalidDaysException {
    _warehouse.advanceDate(offset);
    
  }

  public void registerPartner(String id, String name, String address) throws DuplicatePartnerException {
    _warehouse.registerPartner(id, name, address);
  }

  public Partner getPartnerWithId(String id) throws UnknownPartnerException {
    return _warehouse.getPartnerWithId(id);
  }

  public Collection<Partner> getPartners() {
    return Collections.unmodifiableCollection(_warehouse.getPartners());
  }


  public void registerBreakdownTransaction(String partnerId, String productId, int amount) throws UnknownPartnerException, UnknownProductException, UnavailableProductQuantityException {
    Partner partner = _warehouse.getPartnerWithId(partnerId);
    Product product = _warehouse.getProductWithId(productId);
    _warehouse.registerBreakdownTransaction(partner, product, amount);
  }


  public List<Notification> getPartnerNotifications(String id) throws UnknownPartnerException {
    Partner partner = getPartnerWithId(id);
    return partner.getNotifications();
  }

  public Collection<Product> getProducts() {
    return Collections.unmodifiableCollection(_warehouse.getProducts());
  }

  public void togglePartnerNotifications(String productId, String partnerId) throws UnknownProductException, UnknownPartnerException {
    Product product = _warehouse.getProductWithId(productId);
    ProductObserver observer = _warehouse.getPartnerWithId(partnerId);
    _warehouse.toggleNotifications(product, observer);
  }

  public Collection<Batch> getBatches() {
    return Collections.unmodifiableCollection(_warehouse.getAllBatchesSorted());
  }

  public Collection<Batch> getBatchesFromPartner(String id) throws UnknownPartnerException {
    return Collections.unmodifiableCollection(_warehouse.getBatchesFromPartner(id));
  }

  public Collection<Batch> getBatchesFromProduct(String id) throws UnknownProductException { 
    return Collections.unmodifiableCollection(_warehouse.getBatchesFromProduct(id));
  }

  public Collection<Acquisition> getAcquisitionsFromPartner(String id) throws UnknownPartnerException {
    return Collections.unmodifiableCollection(_warehouse.getAcquisitionsFromPartner(id));
  }

  public Collection<Sale> getSalesFromPartner(String id) throws UnknownPartnerException {
    return Collections.unmodifiableCollection(_warehouse.getSalesFromPartner(id));
  }

  public Transaction getTransactionWithId(int id) throws UnknownTransactionException {
    return _warehouse.getTransactionWithId(id);
  }

  public Collection<Batch> getBatchesUnderGivenPrice(int price) {
    return Collections.unmodifiableCollection(_warehouse.getBatchesUnderGivenPrice(price));
  }

  public Collection<Transaction> getPaymentsPartner(String id) throws UnknownPartnerException {
    return Collections.unmodifiableCollection(_warehouse.getPaymentsPartner(id));
  }

  public boolean productExists(String id) {
    return _warehouse.productExists(id);
  }

  public void registerAcquisitionTransaction(String partnerId, String productId, double price, int quantity) throws UnknownPartnerException, UnknownProductException {
    Partner partner = _warehouse.getPartnerWithId(partnerId);
    Product product = _warehouse.getProductWithId(productId);

    _warehouse.registerAcquisitionTransaction(partner, product, price, quantity);
  }

  public void registerAggregateProduct(String productId, List<String> productIds, List<Integer> quantities, double alpha) throws UnknownProductException {
    List<Product> products = new ArrayList<Product>();

    for(String id : productIds) {
      products.add(_warehouse.getProductWithId(id));
    }
    _warehouse.registerAggregateProduct(productId, products, quantities, alpha);
  }

  public void registerSimpleProduct(String productId) {
    _warehouse.registerSimpleProduct(productId);
  }

  public void pay(int transactionId) throws UnknownTransactionException {
    Transaction transaction = _warehouse.getTransactionWithId(transactionId);
    _warehouse.pay(transaction);
  }

  public double getAvailableBalance() {
    return _warehouse.getAvailableBalance();
  }

  public double getAccountingBalance() {
    return _warehouse.getAccountingBalance();
  }
  

  public void registerSaleTransaction(String partnerId, String productId, int deadline, int amount) throws UnknownPartnerException, UnknownProductException, UnavailableProductQuantityException {
    Partner partner = _warehouse.getPartnerWithId(partnerId);
    Product product = _warehouse.getProductWithId(productId);

    _warehouse.registerSaleTransaction(partner, product, deadline, amount);
  }

}
