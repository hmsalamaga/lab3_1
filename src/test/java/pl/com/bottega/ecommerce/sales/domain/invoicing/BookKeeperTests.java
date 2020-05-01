package pl.com.bottega.ecommerce.sales.domain.invoicing;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import pl.com.bottega.ecommerce.canonicalmodel.publishedlanguage.ClientData;
import pl.com.bottega.ecommerce.canonicalmodel.publishedlanguage.Id;
import pl.com.bottega.ecommerce.sales.domain.productscatalog.Product;
import pl.com.bottega.ecommerce.sales.domain.productscatalog.ProductData;
import pl.com.bottega.ecommerce.sales.domain.productscatalog.ProductType;
import pl.com.bottega.ecommerce.sharedkernel.Money;

import java.math.BigDecimal;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.any;

public class BookKeeperTests {

    BookKeeper bookKeeper = new BookKeeper(new InvoiceFactory());
    InvoiceRequest invoiceRequest;

    @Before
    public void initInvoiceRequest() {
        invoiceRequest = new InvoiceRequest(new ClientData(Id.generate(), "CBA"));
    }

    @Test
    public void expectedInvoiceWithOneElement() {
        Money money = new Money(BigDecimal.valueOf(111));
        Product product = new Product(Id.generate(), money, "Aspirin", ProductType.DRUG);
        ProductData productData = product.generateSnapshot();

        invoiceRequest.add(new RequestItem(productData, 5, money));

        TaxPolicy taxPolicyStub = Mockito.mock(TaxPolicy.class);
        Mockito.when(taxPolicyStub.calculateTax(any(ProductType.class), any(Money.class))).thenReturn(new Tax(money, "Tax"));

        Invoice invoice = bookKeeper.issuance(invoiceRequest, taxPolicyStub);
        assertThat(1, equalTo(invoice.getItems().size()));
    }

    @Test
    public void expectedInvoiceWithoutElements() {
        TaxPolicy taxPolicyStub = Mockito.mock(TaxPolicy.class);
        Mockito.when(taxPolicyStub.calculateTax(any(ProductType.class), any(Money.class)))
               .thenReturn(new Tax(new Money(BigDecimal.valueOf(111)), "Tax"));

        Invoice invoice = bookKeeper.issuance(invoiceRequest, taxPolicyStub);
        assertThat(0, equalTo(invoice.getItems().size()));
    }

    @Test
    public void expectedSpecificInvoiceCost() {
        Money money1 = new Money(BigDecimal.valueOf(111));
        Product product1 = new Product(Id.generate(), money1, "Aspirin", ProductType.DRUG);
        ProductData productData1 = product1.generateSnapshot();

        invoiceRequest.add(new RequestItem(productData1, 1, money1));

        Money money2 = new Money(BigDecimal.valueOf(222));
        Product product2 = new Product(Id.generate(), money2, "Banana", ProductType.FOOD);
        ProductData productData2 = product2.generateSnapshot();

        invoiceRequest.add(new RequestItem(productData2, 2, money2));

        TaxPolicy taxPolicyStub = Mockito.mock(TaxPolicy.class);
        Mockito.when(taxPolicyStub.calculateTax(any(ProductType.class), any(Money.class))).thenReturn(new Tax(money1, "Tax"));

        Invoice invoice = bookKeeper.issuance(invoiceRequest, taxPolicyStub);

        assertThat(invoice.getNet(), equalTo(money1.add(money2)));
    }

    @Test
    public void expectedTwoCallsOfCalculateTax() {
        Money money1 = new Money(BigDecimal.valueOf(111));
        Product product1 = new Product(Id.generate(), money1, "Aspirin", ProductType.DRUG);
        ProductData productData1 = product1.generateSnapshot();

        invoiceRequest.add(new RequestItem(productData1, 1, money1));

        Money money2 = new Money(BigDecimal.valueOf(222));
        Product product2 = new Product(Id.generate(), money2, "Banana", ProductType.FOOD);
        ProductData productData2 = product2.generateSnapshot();

        invoiceRequest.add(new RequestItem(productData2, 2, money2));

        TaxPolicy taxPolicy = Mockito.mock(TaxPolicy.class);
        Mockito.when(taxPolicy.calculateTax(any(ProductType.class), any(Money.class))).thenReturn(new Tax(money1, "Tax"));

        bookKeeper.issuance(invoiceRequest, taxPolicy);

        Mockito.verify(taxPolicy).calculateTax(ProductType.DRUG, money1);
        Mockito.verify(taxPolicy).calculateTax(ProductType.FOOD, money2);
        Mockito.verify(taxPolicy, Mockito.times(2)).calculateTax(any(ProductType.class), any(Money.class));
    }

    @Test
    public void expectedZeroCallsOfCalculateTax() {
        TaxPolicy taxPolicy = Mockito.mock(TaxPolicy.class);
        Mockito.when(taxPolicy.calculateTax(any(ProductType.class), any(Money.class)))
               .thenReturn(new Tax(new Money(BigDecimal.valueOf(111)), "Tax"));

        bookKeeper.issuance(invoiceRequest, taxPolicy);

        Mockito.verify(taxPolicy, Mockito.times(0)).calculateTax(any(ProductType.class), any(Money.class));
    }

    @Test
    public void expectedZeroCallsOfCalculate() {
        Money money = new Money(BigDecimal.valueOf(111));
        Product product = new Product(Id.generate(), money, "Aspirin", ProductType.DRUG);
        ProductData productData = product.generateSnapshot();

        RequestItem requestItem = Mockito.mock(RequestItem.class);
        Mockito.when(requestItem.getProductData()).thenReturn(productData);
        Mockito.when(requestItem.getTotalCost()).thenReturn(money);
        Mockito.when(requestItem.getQuantity()).thenReturn(5);

        invoiceRequest.add(requestItem);

        TaxPolicy taxPolicy = Mockito.mock(TaxPolicy.class);
        Mockito.when(taxPolicy.calculateTax(any(ProductType.class), any(Money.class))).thenReturn(new Tax(money, "Tax"));

        bookKeeper.issuance(invoiceRequest, taxPolicy);

        Mockito.verify(requestItem, Mockito.atMost(2)).getProductData();
    }
}
