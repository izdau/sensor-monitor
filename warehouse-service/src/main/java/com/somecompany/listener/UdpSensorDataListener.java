package com.somecompany.listener;

import com.somecompany.enums.SensorType;
import com.somecompany.service.SensorMeasurementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;
import reactor.netty.Connection;
import reactor.netty.udp.UdpServer;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

/** Receives the sensor data: one UDP port per sensor type. */
@Component
@Slf4j
@RequiredArgsConstructor
public class UdpSensorDataListener implements SmartLifecycle {

    @Value("${conf.temperature-port}")
    private int temperaturePort;
    @Value("${conf.humidity-port}")
    private int humidityPort;

    private final SensorMeasurementService measurementService;
    private final List<Connection> servers = new ArrayList<>();

    @Override
    public void start() {
        listen(SensorType.TEMPERATURE, temperaturePort);
        listen(SensorType.HUMIDITY, humidityPort);
    }

    private void listen(SensorType type, int port) {
        servers.add(
                UdpServer.create()
                .bindAddress(() -> new InetSocketAddress(port)) // all interfaces, IPv4 and IPv6 (the default is localhost only)
                .handle((in, out) -> measurementService.process(type, in.receive().asString()))
                .bindNow());
        log.info("listening for {} sensor on UDP port {}", type, port);
    }

    @Override
    public void stop() {
        servers.forEach(Connection::disposeNow);
        servers.clear();
    }

    @Override
    public boolean isRunning() {
        return !servers.isEmpty();
    }
}
