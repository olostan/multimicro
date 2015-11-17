
package name.olostan.dispatcher;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;
import name.olostan.renderer.RendererGrpc;
import name.olostan.renderer.RendererOuterClass;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Server that manages startup/shutdown of a {@code Greeter} server.
 */
public class DispatcherServer {
  private static final Logger logger = Logger.getLogger(DispatcherServer.class.getName());

  /* The port on which the server should run */
  private int port = 50051;
  private Server server;

  private void start() throws Exception {
    server = ServerBuilder.forPort(port)
        .addService(DispatcherGrpc.bindService(new DispatcherImpl()))
        .build()
        .start();
    logger.info("Dispatcher Server started, listening on " + port);
    Runtime.getRuntime().addShutdownHook(new Thread() {
      @Override
      public void run() {
        // Use stderr here since the logger may have been reset by its JVM shutdown hook.
        System.err.println("*** shutting down gRPC server since JVM is shutting down");
        DispatcherServer.this.stop();
        System.err.println("*** server shut down");
      }
    });
  }

  private void stop() {
    if (server != null) {
      server.shutdown();
    }
  }

  /**
   * Await termination on the main thread since the grpc library uses daemon threads.
   */
  private void blockUntilShutdown() throws InterruptedException {
    if (server != null) {
      server.awaitTermination();
    }
  }

  /**
   * Main launches the server from the command line.
   */
  public static void main(String[] args) throws Exception {
    final DispatcherServer server = new DispatcherServer();
    server.start();
    server.blockUntilShutdown();
  }

  private class DispatcherImpl implements DispatcherGrpc.Dispatcher {

    private final ManagedChannel channel;
    private final RendererGrpc.RendererStub stub;

    public DispatcherImpl() {
      String rendererUrl = System.getenv("RENDERER");
      if (rendererUrl==null) rendererUrl = "localhost";
      logger.info("Opening channel to renderer at "+rendererUrl);
      channel = ManagedChannelBuilder.forAddress(rendererUrl, 50052)
              .usePlaintext(true)
              .build();
      stub = RendererGrpc.newStub(channel);
      // TODO: channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);

    }



    @Override
    public void dispatch(DispatcherOuterClass.DispatchRequest request, StreamObserver<DispatcherOuterClass.DispatchReply> responseObserver) {
      logger.info("Dispatching request to renderer");
      RendererOuterClass.RenderRequest renderRequest =
              RendererOuterClass.RenderRequest.newBuilder().setTexture(request.getImage()).build();
      StreamObserver<RendererOuterClass.RenderResponse> renderResult = new StreamObserver<RendererOuterClass.RenderResponse>() {
        DispatcherOuterClass.DispatchReply reply;
        @Override
        public void onNext(RendererOuterClass.RenderResponse renderResponse) {
           reply = DispatcherOuterClass.DispatchReply.newBuilder()
                  .setImage(renderResponse.getImage())
                  .build();
          responseObserver.onNext(reply);
        }

        @Override
        public void onError(Throwable throwable) {

        }

        @Override
        public void onCompleted() {
          responseObserver.onCompleted();
        }
      };
      stub.render(renderRequest,renderResult);


    }

  }
}
