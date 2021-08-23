/* This code was initialised by Akka Serverless tooling.
 * As long as this file exists it will not be re-generated.
 * You are free to make changes to this file.
 */

package customer.domain;

import com.akkaserverless.javasdk.eventsourcedentity.CommandContext;
import com.google.protobuf.Empty;
import customer.api.CustomerApi;
import org.junit.Test;
import org.mockito.*;

import static org.junit.Assert.assertThrows;

public class CustomerEntityTest {
    private String entityId = "entityId1";
    private CustomerEntity entity;
    private CommandContext context = Mockito.mock(CommandContext.class);
    
    @Test
    public void createTest() {
        entity = new CustomerEntity(entityId);
        
        // TODO: write your mock here
        // Mockito.when(context.[...]).thenReturn([...]);
        
        // TODO: set fields in command, and update assertions to verify implementation
        // assertEquals([expected],
        //    entity.create(CustomerApi.Customer.newBuilder().build(), context);
        // );
        
        // TODO: if you wish to verify events:
        //    Mockito.verify(context).emit(event);
    }
    
    @Test
    public void changeNameTest() {
        entity = new CustomerEntity(entityId);
        
        // TODO: write your mock here
        // Mockito.when(context.[...]).thenReturn([...]);
        
        // TODO: set fields in command, and update assertions to verify implementation
        // assertEquals([expected],
        //    entity.changeName(CustomerApi.ChangeNameRequest.newBuilder().build(), context);
        // );
        
        // TODO: if you wish to verify events:
        //    Mockito.verify(context).emit(event);
    }
    
    @Test
    public void changeAddressTest() {
        entity = new CustomerEntity(entityId);
        
        // TODO: write your mock here
        // Mockito.when(context.[...]).thenReturn([...]);
        
        // TODO: set fields in command, and update assertions to verify implementation
        // assertEquals([expected],
        //    entity.changeAddress(CustomerApi.ChangeAddressRequest.newBuilder().build(), context);
        // );
        
        // TODO: if you wish to verify events:
        //    Mockito.verify(context).emit(event);
    }
    
    @Test
    public void getCustomerTest() {
        entity = new CustomerEntity(entityId);
        
        // TODO: write your mock here
        // Mockito.when(context.[...]).thenReturn([...]);
        
        // TODO: set fields in command, and update assertions to verify implementation
        // assertEquals([expected],
        //    entity.getCustomer(CustomerApi.GetCustomerRequest.newBuilder().build(), context);
        // );
        
        // TODO: if you wish to verify events:
        //    Mockito.verify(context).emit(event);
    }
}