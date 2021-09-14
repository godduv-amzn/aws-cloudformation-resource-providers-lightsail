package software.amazon.lightsail.instance.helpers.resource;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import lombok.val;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import software.amazon.awssdk.awscore.AwsRequest;
import software.amazon.awssdk.services.lightsail.LightsailClient;
import software.amazon.awssdk.services.lightsail.model.GetInstanceRequest;
import software.amazon.awssdk.services.lightsail.model.GetInstanceResponse;
import software.amazon.awssdk.services.lightsail.model.InstanceState;
import software.amazon.awssdk.services.lightsail.model.NotFoundException;
import software.amazon.awssdk.services.lightsail.model.TagResourceRequest;
import software.amazon.awssdk.services.lightsail.model.UntagResourceRequest;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.LoggerProxy;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.lightsail.instance.AbstractTestBase;
import software.amazon.lightsail.instance.Port;
import software.amazon.lightsail.instance.ResourceModel;
import software.amazon.lightsail.instance.State;
import software.amazon.lightsail.instance.Tag;

import java.time.Duration;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.amazon.lightsail.instance.AbstractTestBase.MOCK_CREDENTIALS;

class TagsTest {

    private Tags tagsTest;

    @Mock
    private AmazonWebServicesClientProxy proxy;

    private LightsailClient sdkClient;

    @BeforeEach
    public void setup() {

        final ResourceModel model = ResourceModel.builder()
                .addOns(new ArrayList<>())
                .state(State.builder()
                        .name("Running")
                        .build())
                .networking(software.amazon.lightsail.instance.Networking.builder()
                        .ports(ImmutableSet.of(
                                Port.builder()
                                        .ipv6Cidrs(ImmutableList.of("2.2.24.2.3.2323.232323:/dsds"))
                                        .accessFrom("1")
                                        .cidrs(ImmutableList.of("1.2.2.232"))
                                        .accessDirection("one")
                                        .fromPort(20)
                                        .toPort(40)
                                        .cidrListAliases(ImmutableList.of("1.2.2.2"))
                                        .build()))
                        .monthlyTransfer(software.amazon.lightsail.instance.MonthlyTransfer.builder()
                                .gbPerMonthAllocated("20").build())
                        .build())
                .tags(ImmutableSet.of(
                        Tag.builder().key("key1").value("value1").build(),
                        Tag.builder().key("key2").value("value2").build(),
                        Tag.builder().key("key3").value("value3").build(),
                        Tag.builder().key("key1").value("value2").build(),
                        Tag.builder().key("key4").value("value3").build(),
                        Tag.builder().key("key5").build())
                ).build();
        val logger = mock(Logger.class);
        sdkClient = mock(LightsailClient.class);
        proxy = new AmazonWebServicesClientProxy(mock(LoggerProxy.class), MOCK_CREDENTIALS, () -> Duration.ofSeconds(600).toMillis());
        ProxyClient<LightsailClient> proxyClient = AbstractTestBase.MOCK_PROXY(proxy, sdkClient);
        ResourceHandlerRequest<ResourceModel> resourceModelRequest =
                ResourceHandlerRequest.<ResourceModel>builder()
                        .desiredResourceState(model)
                        .build();
        tagsTest = new Tags(model, logger,
                proxyClient, resourceModelRequest);
    }


    @Test
    public void update() {
        val basicResponse = GetInstanceResponse.builder()
                .instance(software.amazon.awssdk.services.lightsail.model.Instance.builder().state(InstanceState
                        .builder()
                        .name("Running")
                        .build())
                        .tags(ImmutableSet.of(
                                software.amazon.awssdk.services.lightsail.model.Tag.builder()
                                        .key("key1").value("value1").build(),
                                software.amazon.awssdk.services.lightsail.model.Tag.builder()
                                        .key("key2").value("value5").build(), // different value than what is there.
                                software.amazon.awssdk.services.lightsail.model.Tag.builder()
                                        .key("key6").value("value6").build(), // new Key new Value
                                software.amazon.awssdk.services.lightsail.model.Tag.builder()
                                        .key("key6").value("value2").build(), // new Key old Value.
                                software.amazon.awssdk.services.lightsail.model.Tag.builder()
                                        .key("key4").build())) // only old key, no value
                        .build()).build();

        when(sdkClient.getInstance(any(GetInstanceRequest.class)))
                .thenReturn(basicResponse)
                .thenReturn(basicResponse)
                .thenReturn(basicResponse);

        tagsTest.update(mock(AwsRequest.class));

        ArgumentCaptor<TagResourceRequest> captor1 = ArgumentCaptor.forClass(TagResourceRequest.class);
        ArgumentCaptor<UntagResourceRequest> captor2 = ArgumentCaptor.forClass(UntagResourceRequest.class);
        verify(sdkClient, times(1)).tagResource(captor1.capture());
        verify(sdkClient, times(1)).untagResource(captor2.capture());
        assertNotNull(captor1.getValue());
        assertNotNull(captor2.getValue());
        assertNotNull(captor1.getValue().tags());
        assertNotNull(captor2.getValue().tagKeys());
        assertEquals(5, captor1.getValue().tags().size());
        assertEquals(3, captor2.getValue().tagKeys().size());
    }

    @Test
    public void updateOnlyRemove() {
        val basicResponse = GetInstanceResponse.builder()
                .instance(software.amazon.awssdk.services.lightsail.model.Instance.builder().state(InstanceState
                        .builder()
                        .name("Running")
                        .build())
                        .tags(ImmutableSet.of(
                                software.amazon.awssdk.services.lightsail.model.Tag.builder()
                                        .key("key1").value("value1").build(),
                                software.amazon.awssdk.services.lightsail.model.Tag.builder()
                                        .key("key2").value("value2").build(),
                                software.amazon.awssdk.services.lightsail.model.Tag.builder()
                                        .key("key3").value("value3").build(),
                                software.amazon.awssdk.services.lightsail.model.Tag.builder()
                                        .key("key1").value("value2").build(),
                                software.amazon.awssdk.services.lightsail.model.Tag.builder()
                                        .key("key4").value("value3").build(),
                                software.amazon.awssdk.services.lightsail.model.Tag.builder()
                                        .key("key5").build(),
                                software.amazon.awssdk.services.lightsail.model.Tag.builder()
                                        .key("key10").value("value10").build(),
                                software.amazon.awssdk.services.lightsail.model.Tag.builder()
                                        .key("key11").value("value11").build(),
                                software.amazon.awssdk.services.lightsail.model.Tag.builder()
                                        .key("key12").value("value12").build()))
                        .build()).build();

        when(sdkClient.getInstance(any(GetInstanceRequest.class)))
                .thenReturn(basicResponse)
                .thenReturn(basicResponse)
                .thenReturn(basicResponse);

        tagsTest.update(mock(AwsRequest.class));

        ArgumentCaptor<TagResourceRequest> captor1 = ArgumentCaptor.forClass(TagResourceRequest.class);
        ArgumentCaptor<UntagResourceRequest> captor2 = ArgumentCaptor.forClass(UntagResourceRequest.class);
        verify(sdkClient, times(0)).tagResource(captor1.capture());
        verify(sdkClient, times(1)).untagResource(captor2.capture());
        assertNotNull(captor2.getValue());
        assertNotNull(captor2.getValue().tagKeys());
        assertEquals(3, captor2.getValue().tagKeys().size());
    }

    @Test
    public void updateOnlyAdd() {
        val basicResponse = GetInstanceResponse.builder()
                .instance(software.amazon.awssdk.services.lightsail.model.Instance.builder().state(InstanceState
                        .builder()
                        .name("Running")
                        .build())
                        .tags(ImmutableSet.of(
                                software.amazon.awssdk.services.lightsail.model.Tag.builder()
                                        .key("key1").value("value1").build(),
                                software.amazon.awssdk.services.lightsail.model.Tag.builder()
                                        .key("key2").value("value2").build(),
                                software.amazon.awssdk.services.lightsail.model.Tag.builder()
                                        .key("key3").value("value3").build()))
                        .build()).build();

        when(sdkClient.getInstance(any(GetInstanceRequest.class)))
                .thenReturn(basicResponse)
                .thenReturn(basicResponse)
                .thenReturn(basicResponse);

        tagsTest.update(mock(AwsRequest.class));

        ArgumentCaptor<TagResourceRequest> captor1 = ArgumentCaptor.forClass(TagResourceRequest.class);
        ArgumentCaptor<UntagResourceRequest> captor2 = ArgumentCaptor.forClass(UntagResourceRequest.class);
        verify(sdkClient, times(1)).tagResource(captor1.capture());
        verify(sdkClient, times(0)).untagResource(captor2.capture());
        assertNotNull(captor1.getValue());
        assertNotNull(captor1.getValue().tags());
        assertEquals(3, captor1.getValue().tags().size());
    }


    @Test
    public void testUnSupportedMethods() {
        try {
            tagsTest.create(mock(AwsRequest.class));
            fail();
        } catch (UnsupportedOperationException e) {
            // pass the exception
        }

        try {
            tagsTest.delete(mock(AwsRequest.class));
            fail();
        } catch (UnsupportedOperationException e) {
            // pass the exception
        }

        try {
            tagsTest.read(mock(AwsRequest.class));
            fail();
        } catch (UnsupportedOperationException e) {
            // pass the exception
        }

        try {
            tagsTest.isStabilizedUpdate();
            fail();
        } catch (UnsupportedOperationException e) {
            // pass the exception
        }

        try {
            tagsTest.isStabilizedDelete();
            fail();
        } catch (UnsupportedOperationException e) {
            // pass the exception
        }

        try {
            tagsTest.isSafeExceptionCreateOrUpdate(NotFoundException.builder().build());
            fail();
        } catch (UnsupportedOperationException e) {
            // pass the exception
        }

        try {
            tagsTest.isSafeExceptionDelete(NotFoundException.builder().build());
            fail();
        } catch (UnsupportedOperationException e) {
            // pass the exception
        }
    }
}
