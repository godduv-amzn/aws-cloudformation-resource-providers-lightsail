package software.amazon.lightsail.container;

import com.google.common.collect.ImmutableList;
import lombok.val;
import software.amazon.awssdk.services.lightsail.LightsailClient;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.lightsail.container.helpers.resource.Container;

public class ReadHandler extends BaseHandlerStd {
    private Logger logger;

    protected Container getContainer(final ResourceHandlerRequest<ResourceModel> request,
                                           final ProxyClient<LightsailClient> proxyClient, final Logger logger) {
        return new Container(request.getDesiredResourceState(), logger, proxyClient, request);
    }

    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final ProxyClient<LightsailClient> proxyClient,
            final Logger logger) {

        this.logger = logger;
        val container = getContainer(request, proxyClient, logger);

        return proxy
                .initiate("AWS-Lightsail-Container::Read", proxyClient, request.getDesiredResourceState(), callbackContext)
                .translateToServiceRequest(Translator::translateToReadRequest)
                .makeServiceCall((awsRequest, client) -> container.read(awsRequest))
                .handleError((awsRequest, exception, client, model, context) -> handleError(exception, model,
                        callbackContext, ImmutableList.of(), logger, this.getClass().getSimpleName()))
                .done(awsResponse -> ProgressEvent
                        .defaultSuccessHandler(Translator.translateFromReadResponse(awsResponse)));
    }
}