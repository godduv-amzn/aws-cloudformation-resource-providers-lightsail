package software.amazon.lightsail.loadbalancertlscertificate.helpers.resource;

import lombok.val;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.lightsail.LightsailClient;
import software.amazon.awssdk.services.lightsail.model.*;
import software.amazon.cloudformation.exceptions.CfnAlreadyExistsException;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.cloudformation.proxy.*;
import software.amazon.lightsail.loadbalancertlscertificate.AbstractTestBase;
import software.amazon.lightsail.loadbalancertlscertificate.ResourceModel;

import java.time.Duration;
import java.util.Arrays;
import java.util.HashSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.*;
import static software.amazon.lightsail.loadbalancertlscertificate.AbstractTestBase.MOCK_CREDENTIALS;

@ExtendWith(MockitoExtension.class)
public class LoadBalancerTlsCertificateTest {

    @Mock
    private AmazonWebServicesClientProxy proxy;

    @Mock
    private ProxyClient<LightsailClient> proxyClient;

    @Mock
    LightsailClient sdkClient;

    private Logger logger;
    private LoadBalancerTlsCertificate testLoadBalancerTlsCertificate;

    @BeforeEach
    public void setup() {
        proxy = new AmazonWebServicesClientProxy(mock(LoggerProxy.class), MOCK_CREDENTIALS, () -> Duration.ofSeconds(600).toMillis());
        sdkClient = mock(LightsailClient.class);
        proxyClient = AbstractTestBase.MOCK_PROXY(proxy, sdkClient);
        logger = mock(Logger.class);

        final ResourceModel model = ResourceModel.builder().certificateName("testCert").build();
        ResourceHandlerRequest<ResourceModel> resourceModelRequest =
                ResourceHandlerRequest.<ResourceModel>builder()
                        .desiredResourceState(model)
                        .build();
        testLoadBalancerTlsCertificate = new LoadBalancerTlsCertificate(model, logger, proxyClient, resourceModelRequest);
    }

    @AfterEach
    public void tear_down() {

    }

    @Test
    public void testCreate() {
        when(sdkClient.createLoadBalancerTlsCertificate(any(CreateLoadBalancerTlsCertificateRequest.class)))
                .thenReturn(CreateLoadBalancerTlsCertificateResponse.builder().build());
        val result = testLoadBalancerTlsCertificate.create(CreateLoadBalancerTlsCertificateRequest.builder().build());
        verify(sdkClient, times(1)).createLoadBalancerTlsCertificate(any(CreateLoadBalancerTlsCertificateRequest.class));
        assertThat(result).isNotNull();
    }

    @Test
    public void testCreateValidates() {
        LoadBalancerTlsCertificate testLoadBalancerTlsCertificateSpy = spy(testLoadBalancerTlsCertificate);
        when(sdkClient.createLoadBalancerTlsCertificate(any(CreateLoadBalancerTlsCertificateRequest.class)))
                .thenReturn(CreateLoadBalancerTlsCertificateResponse.builder().build());
        val result = testLoadBalancerTlsCertificateSpy.create(CreateLoadBalancerTlsCertificateRequest.builder().build());
        verify(testLoadBalancerTlsCertificateSpy, times(1)).validateTemplate(any(ResourceModel.class));
    }

    @Test
    public void testRead() {
        when(sdkClient.getLoadBalancerTlsCertificates(any(GetLoadBalancerTlsCertificatesRequest.class)))
                .thenReturn(GetLoadBalancerTlsCertificatesResponse.builder().tlsCertificates(software.amazon.awssdk.services.lightsail.model.LoadBalancerTlsCertificate.
                        builder().name("testCert").build()).build());
        when(sdkClient.getLoadBalancer(any(GetLoadBalancerRequest.class)))
                .thenReturn(GetLoadBalancerResponse.builder().loadBalancer(software.amazon.awssdk.services.lightsail.model.LoadBalancer.builder()
                        .name("testLb").httpsRedirectionEnabled(false).build()).build());
        val result = testLoadBalancerTlsCertificate.read(GetLoadBalancerTlsCertificatesRequest.builder().build());
        verify(sdkClient, times(1)).getLoadBalancerTlsCertificates(any(GetLoadBalancerTlsCertificatesRequest.class));
        assertThat(result).isNotNull();
    }

    @Test
    public void testDelete() {
        when(sdkClient.deleteLoadBalancerTlsCertificate(any(DeleteLoadBalancerTlsCertificateRequest.class)))
                .thenReturn(DeleteLoadBalancerTlsCertificateResponse.builder().build());
        val result = testLoadBalancerTlsCertificate.delete(DeleteLoadBalancerTlsCertificateRequest.builder().build());
        verify(sdkClient, times(1)).deleteLoadBalancerTlsCertificate(any(DeleteLoadBalancerTlsCertificateRequest.class));
        assertThat(result).isNotNull();
    }

    @Test
    public void testAttachToLoadBalancer_noAttach1() {
        ResourceModel resourceModel = ResourceModel.builder().build();
        ResourceHandlerRequest<ResourceModel> modelRequest =
                ResourceHandlerRequest.<ResourceModel>builder()
                        .desiredResourceState(resourceModel)
                        .build();
        LoadBalancerTlsCertificate testCert = new LoadBalancerTlsCertificate(resourceModel, logger, proxyClient, modelRequest);
        val result = testCert.attachToLoadBalancer();
        verify(sdkClient, never()).attachInstancesToLoadBalancer(any(AttachInstancesToLoadBalancerRequest.class));
    }

    @Test
    public void testAttachToLoadBalancer_noAttach2() {
        ResourceModel resourceModel = ResourceModel.builder().isAttached(false).build();
        ResourceHandlerRequest<ResourceModel> modelRequest =
                ResourceHandlerRequest.<ResourceModel>builder()
                        .desiredResourceState(resourceModel)
                        .build();
        LoadBalancerTlsCertificate testCert = new LoadBalancerTlsCertificate(resourceModel, logger, proxyClient, modelRequest);
        val result = testCert.attachToLoadBalancer();
        verify(sdkClient, never()).attachInstancesToLoadBalancer(any(AttachInstancesToLoadBalancerRequest.class));
    }

    @Test
    public void testAttachToLoadBalancer_attach() {
        ResourceModel resourceModel = ResourceModel.builder().isAttached(true).build();
        ResourceHandlerRequest<ResourceModel> modelRequest =
                ResourceHandlerRequest.<ResourceModel>builder()
                        .desiredResourceState(resourceModel)
                        .build();
        LoadBalancerTlsCertificate testCert = new LoadBalancerTlsCertificate(resourceModel, logger, proxyClient, modelRequest);
        val result = testCert.attachToLoadBalancer();
        verify(sdkClient, times(1)).attachLoadBalancerTlsCertificate(any(AttachLoadBalancerTlsCertificateRequest.class));
    }

    @Test
    public void testAttachToLoadBalancerValidates() {
        LoadBalancerTlsCertificate testLoadBalancerTlsCertificateSpy = spy(testLoadBalancerTlsCertificate);
        val result = testLoadBalancerTlsCertificateSpy.attachToLoadBalancer();
        verify(testLoadBalancerTlsCertificateSpy, times(1)).validateTemplate(any(ResourceModel.class));
    }

    @Test
    public void testModifyHttpsRedirection_noModify1() {
        ResourceModel resourceModel = ResourceModel.builder().httpsRedirectionEnabled(true).build();
        ResourceHandlerRequest<ResourceModel> modelRequest =
                ResourceHandlerRequest.<ResourceModel>builder()
                        .desiredResourceState(resourceModel)
                        .build();
        LoadBalancerTlsCertificate testCert = new LoadBalancerTlsCertificate(resourceModel, logger, proxyClient, modelRequest);
        val result = testCert.modifyHttpsRedirection();
        verify(sdkClient, never()).updateLoadBalancerAttribute(any(UpdateLoadBalancerAttributeRequest.class));
    }

    @Test
    public void testModifyHttpsRedirection_noModify2() {
        ResourceModel resourceModel = ResourceModel.builder().isAttached(false).httpsRedirectionEnabled(true).build();
        ResourceHandlerRequest<ResourceModel> modelRequest =
                ResourceHandlerRequest.<ResourceModel>builder()
                        .desiredResourceState(resourceModel)
                        .build();
        LoadBalancerTlsCertificate testCert = new LoadBalancerTlsCertificate(resourceModel, logger, proxyClient, modelRequest);
        val result = testCert.modifyHttpsRedirection();
        verify(sdkClient, never()).updateLoadBalancerAttribute(any(UpdateLoadBalancerAttributeRequest.class));
    }

    @Test
    public void testModifyHttpsRedirection_Modify() {
        ResourceModel resourceModel = ResourceModel.builder().isAttached(true).httpsRedirectionEnabled(true).build();
        ResourceHandlerRequest<ResourceModel> modelRequest =
                ResourceHandlerRequest.<ResourceModel>builder()
                        .desiredResourceState(resourceModel)
                        .build();
        LoadBalancerTlsCertificate testCert = new LoadBalancerTlsCertificate(resourceModel, logger, proxyClient, modelRequest);
        val result = testCert.modifyHttpsRedirection();
        verify(sdkClient, times(1)).updateLoadBalancerAttribute(any(UpdateLoadBalancerAttributeRequest.class));
    }

    @Test
    public void testValidateTemplate_valid1() {
        ResourceModel resourceModel = ResourceModel.builder().isAttached(true).httpsRedirectionEnabled(true).build();
        ResourceHandlerRequest<ResourceModel> modelRequest =
                ResourceHandlerRequest.<ResourceModel>builder()
                        .desiredResourceState(resourceModel)
                        .build();
        LoadBalancerTlsCertificate testCert = new LoadBalancerTlsCertificate(resourceModel, logger, proxyClient, modelRequest);
        try {
            testCert.validateTemplate(resourceModel);
        } catch (CfnAlreadyExistsException ex) {
            fail();
        }
    }

    @Test
    public void testValidateTemplate_valid2() {
        ResourceModel resourceModel = ResourceModel.builder().isAttached(true).httpsRedirectionEnabled(false).build();
        ResourceHandlerRequest<ResourceModel> modelRequest =
                ResourceHandlerRequest.<ResourceModel>builder()
                        .desiredResourceState(resourceModel)
                        .build();
        LoadBalancerTlsCertificate testCert = new LoadBalancerTlsCertificate(resourceModel, logger, proxyClient, modelRequest);
        try {
            testCert.validateTemplate(resourceModel);
        } catch (CfnAlreadyExistsException ex) {
            fail();
        }
    }

    @Test
    public void testValidateTemplate_invalid1() {
        ResourceModel resourceModel = ResourceModel.builder().isAttached(false).httpsRedirectionEnabled(true).build();
        ResourceHandlerRequest<ResourceModel> modelRequest =
                ResourceHandlerRequest.<ResourceModel>builder()
                        .desiredResourceState(resourceModel)
                        .build();
        LoadBalancerTlsCertificate testCert = new LoadBalancerTlsCertificate(resourceModel, logger, proxyClient, modelRequest);
        try {
            testCert.validateTemplate(resourceModel);
            fail();
        } catch (CfnInvalidRequestException ex) {
            // Exception expected.
        }
    }

    @Test
    public void testValidateTemplate_invalid2() {
        ResourceModel resourceModel = ResourceModel.builder().httpsRedirectionEnabled(true).build();
        ResourceHandlerRequest<ResourceModel> modelRequest =
                ResourceHandlerRequest.<ResourceModel>builder()
                        .desiredResourceState(resourceModel)
                        .build();
        LoadBalancerTlsCertificate testCert = new LoadBalancerTlsCertificate(resourceModel, logger, proxyClient, modelRequest);
        try {
            testCert.validateTemplate(resourceModel);
            fail();
        } catch (CfnInvalidRequestException ex) {
            // Exception expected.
        }
    }

    @Test
    public void testIsStabilizedCreate_stabilized() {
        when(sdkClient.getLoadBalancerTlsCertificates(any(GetLoadBalancerTlsCertificatesRequest.class)))
                .thenReturn(GetLoadBalancerTlsCertificatesResponse.builder()
                        .tlsCertificates(software.amazon.awssdk.services.lightsail.model.LoadBalancerTlsCertificate
                        .builder().name("testCert").build()).build());
        when(sdkClient.getLoadBalancer(any(GetLoadBalancerRequest.class)))
                .thenReturn(GetLoadBalancerResponse.builder().loadBalancer(software.amazon.awssdk.services.lightsail.model.LoadBalancer.builder()
                        .name("testLb").httpsRedirectionEnabled(false).build()).build());
        val result = testLoadBalancerTlsCertificate.isStabilizedCreate();
        verify(sdkClient, times(1)).getLoadBalancerTlsCertificates(any(GetLoadBalancerTlsCertificatesRequest.class));
        assertThat(result).isTrue();
    }

    @Test
    public void testIsStabilizedCreate_notStabilized() {
        when(sdkClient.getLoadBalancerTlsCertificates(any(GetLoadBalancerTlsCertificatesRequest.class)))
                .thenReturn(GetLoadBalancerTlsCertificatesResponse.builder().build());
        val result = testLoadBalancerTlsCertificate.isStabilizedCreate();
        verify(sdkClient, times(1)).getLoadBalancerTlsCertificates(any(GetLoadBalancerTlsCertificatesRequest.class));
        assertThat(result).isFalse();
    }

    @Test
    public void testIsStabilizedDelete_stabilized() {
        when(sdkClient.getLoadBalancerTlsCertificates(any(GetLoadBalancerTlsCertificatesRequest.class)))
                .thenReturn(GetLoadBalancerTlsCertificatesResponse.builder().build());
        val result = testLoadBalancerTlsCertificate.isStabilizedDelete();
        verify(sdkClient, times(1)).getLoadBalancerTlsCertificates(any(GetLoadBalancerTlsCertificatesRequest.class));
        assertThat(result).isTrue();
    }

    @Test
    public void testIsStabilizedDelete_notStabilized() {
        when(sdkClient.getLoadBalancerTlsCertificates(any(GetLoadBalancerTlsCertificatesRequest.class)))
                .thenReturn(GetLoadBalancerTlsCertificatesResponse.builder()
                        .tlsCertificates(software.amazon.awssdk.services.lightsail.model.LoadBalancerTlsCertificate
                                .builder().name("testCert").build()).build());
        when(sdkClient.getLoadBalancer(any(GetLoadBalancerRequest.class)))
                .thenReturn(GetLoadBalancerResponse.builder().loadBalancer(software.amazon.awssdk.services.lightsail.model.LoadBalancer.builder()
                        .name("testLb").httpsRedirectionEnabled(false).build()).build());
        val result = testLoadBalancerTlsCertificate.isStabilizedDelete();
        verify(sdkClient, times(1)).getLoadBalancerTlsCertificates(any(GetLoadBalancerTlsCertificatesRequest.class));
        assertThat(result).isFalse();
    }

}
