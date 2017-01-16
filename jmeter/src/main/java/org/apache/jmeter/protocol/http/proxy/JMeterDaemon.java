package org.apache.jmeter.protocol.http.proxy;

import org.apache.jmeter.gui.Stoppable;
import org.apache.jorphan.util.JOrphanUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

/**
 * Copy past of org.apache.jmeter.protocol.http.proxy.Daemon
 * Created to modify jmeter proxy creation
 *
 * @author Denis V. Kirpichenkov
 */
public class JMeterDaemon extends Thread implements Stoppable
{

    private static final Logger LOG = LoggerFactory.getLogger(JMeterProxyControl.class);;

    /**
     * The time (in milliseconds) to wait when accepting a client connection.
     * The accept will be retried until the Daemon is told to stop. So this
     * interval is the longest time that the Daemon will have to wait after
     * being told to stop.
     */
    private static final int ACCEPT_TIMEOUT = 20000;

    /**
     * The port to listen on.
     */
    private int daemonPort;

    private ServerSocket mainSocket;

    /**
     * True if the Daemon is currently running.
     */
    private volatile boolean running;

    /**
     * The target which will receive the generated JMeter test components.
     */
    private JMeterProxyControl target;

    /**
     * The proxy class which will be used to handle individual requests. This
     * class must be the {@link Proxy} class or a subclass.
     */
    private Class<? extends JMeterProxy> proxyClass;

    /**
     * Create a new Daemon with the specified port and target.
     *
     * @param port   the port to listen on.
     * @param target the target which will receive the generated JMeter test
     *               components.
     * @throws IOException              if an I/O error occurs opening the socket
     * @throws IllegalArgumentException if <code>port</code> is outside the allowed range from <code>0</code> to <code>65535</code>
     * @throws SocketException          when something is wrong on the underlying protocol layer
     */
    public JMeterDaemon(int port, JMeterProxyControl target) throws IOException
    {
        this(port, target, JMeterProxy.class);
    }

    /**
     * Create a new Daemon with the specified port and target, using the
     * specified class to handle individual requests.
     *
     * @param port       the port to listen on.
     * @param target     the target which will receive the generated JMeter test
     *                   components.
     * @param proxyClass the proxy class to use to handle individual requests. This
     *                   class must be the {@link Proxy} class or a subclass.
     * @throws IOException              if an I/O error occurs opening the socket
     * @throws IllegalArgumentException if <code>port</code> is outside the allowed range from <code>0</code> to <code>65535</code>
     * @throws SocketException          when something is wrong on the underlying protocol layer
     */
    public JMeterDaemon(int port, JMeterProxyControl target, Class<? extends JMeterProxy> proxyClass)
            throws IOException
    {
        super("HTTP Proxy Daemon");
        this.target = target;
        this.daemonPort = port;
        this.proxyClass = proxyClass;
        LOG.info("Creating Daemon Socket on port: " + daemonPort);
        mainSocket = new ServerSocket(daemonPort);
        mainSocket.setSoTimeout(ACCEPT_TIMEOUT);
    }

    /**
     * Listen on the daemon port and handle incoming requests. This method will
     * not exit until {@link #stopServer()} is called or an error occurs.
     */
    @Override
    public void run()
    {
        running = true;
        LOG.info("Test Script Recorder up and running!");

        try
        {
            while (running)
            {
                try
                {
                    // Listen on main socket
                    Socket clientSocket = mainSocket.accept();
                    if (running)
                    {
                        // Pass request to new proxy thread
                        JMeterProxy thd = proxyClass.newInstance();
                        thd.configure(clientSocket, target);
                        thd.start();
                    }
                }
                catch (InterruptedIOException e)
                {
                    continue;
                    // Timeout occurred. Ignore, and keep looping until we're
                    // told to stop running.
                }
            }
            LOG.info("HTTP(S) Test Script Recorder stopped");
        }
        catch (Exception e)
        {
            LOG.warn("HTTP(S) Test Script Recorder stopped", e);
        }
        finally
        {
            JOrphanUtils.closeQuietly(mainSocket);
        }
    }

    /**
     * Stop the proxy daemon. The daemon may not stop immediately.
     * <p>
     * see #ACCEPT_TIMEOUT
     */
    @Override
    public void stopServer()
    {
        running = false;
    }
}
