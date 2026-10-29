package com.somecompany.listener;

import com.somecompany.enums.SensorType;
import com.somecompany.service.SensorMeasurementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Receives the sensor data: one UDP port per sensor type, one virtual thread per sensor data. */
@Component
@Slf4j
@RequiredArgsConstructor
public class UdpSensorDataListener implements SmartLifecycle {

    @Value("${conf.sensor.temperature-port}")
    private int temperaturePort;
    @Value("${conf.sensor.humidity-port}")
    private int humidityPort;

    private final SensorMeasurementService sensorMeasurementService;
    private final List<DatagramSocket> sockets = new ArrayList<>();
    private ExecutorService executor;

    @Override
    public void start() {
        executor = Executors.newVirtualThreadPerTaskExecutor();
        listen(SensorType.TEMPERATURE, temperaturePort);
        listen(SensorType.HUMIDITY, humidityPort);
    }

    private void listen(SensorType type, int port) {
        try {
            DatagramSocket socket = new DatagramSocket(port); // all interfaces, IPv4 and IPv6
            sockets.add(socket);
            executor.submit(() -> receive(type, socket));
        } catch (SocketException e) {
            throw new IllegalStateException("cannot listen on UDP port " + port, e);
        }
        log.info("listening for {} sensor on UDP port {}", type, port);
    }

    private void receive(SensorType type, DatagramSocket socket) {
        while (!socket.isClosed()) {
            try {
                DatagramPacket packet = new DatagramPacket(new byte[1024], 1024);
                socket.receive(packet); // blocks only this virtual thread
                String sensorData = new String(packet.getData(), 0, packet.getLength(), StandardCharsets.UTF_8);
                executor.submit(() -> sensorMeasurementService.process(type, sensorData)); // waiting for Kafka does not hold up the next sensor data
            } catch (IOException e) {
                if (!socket.isClosed()) // closed by stop()
                    log.warn("{} sensor data not received: {}", type, e.getMessage());
            }
        }
    }

    @Override
    public void stop() {
        sockets.forEach(DatagramSocket::close); // ends the receive loops
        sockets.clear();
        executor.close(); // waits for the sensor data in progress
    }

    @Override
    public boolean isRunning() {
        return !sockets.isEmpty();
    }
}
