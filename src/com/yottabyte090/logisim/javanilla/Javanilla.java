/*
 * MIT License
 *
 * Copyright (c) 2018 Sangwon Ryu
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.yottabyte090.logisim.javanilla;

import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.instance.InstanceFactory;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.instance.Port;

import java.awt.*;
import java.awt.font.TextAttribute;

import java.text.AttributedString;
import java.util.Queue;
import java.util.LinkedList;

/**
 * @author Sangwon Ryu <yottabyte090 at naver.com>
 * @since 2018-10-25
 */

public class Javanilla extends InstanceFactory {
    private Queue <Task> taskQueue = new LinkedList <> ();
    private Register[] registers = new Register[16];
    private int programCounter = 0;
    private long compareA;
    private long compareB;

    private Value lastClock = Value.FALSE;

    private static final int CLOCK = 0;
    private static final int RESET = 1;
    private static final int MEMORY = 2;
    private static final int ADDRESS = 3;
    private static final int STDIN = 4;
    private static final int STDOUT = 5;

    private static final int DELAY = 50;

    Javanilla() {
        super("Ja-Vanilla");

        setAttributes(new Attribute[] {

        }, new Object[] {

        });

        setOffsetBounds(Bounds.create(0, 0, 160, 90));

        setPorts(new Port[] {
                new Port(0, 10, Port.INPUT, 1), // CLOCK
                new Port(0, 30, Port.INPUT, 1), // RESET
                new Port(0, 60, Port.INOUT, 32), // MEMORY
                new Port(0, 80, Port.OUTPUT, 17), // ADDRESS
                new Port(160, 10, Port.INPUT, 32), // STDIN
                new Port(160, 30, Port.OUTPUT, 32) // STDOUT
        });

        for (int i = 0; i < 16; i++) {
            registers[i] = new Register();
        }
    }

    @Override
    public void paintInstance(InstancePainter painter) {
        Graphics g = painter.getGraphics();
        // Bounds b = painter.getBounds(); - DEAD CODE

        int x = painter.getBounds().getX();
        int y = painter.getBounds().getY();

        painter.drawRectangle(painter.getBounds(), "");

        AttributedString projectName = new AttributedString("Ja-Vanilla");
        AttributedString authorName = new AttributedString("by Yottabyte090");
        projectName.addAttribute(TextAttribute.FOREGROUND, Color.gray);
        authorName.addAttribute(TextAttribute.FOREGROUND, Color.gray);

        g.drawString(projectName.getIterator(), x + 101, y + 72);
        g.drawString(authorName.getIterator(), x + 70, y + 85);
        g.drawString("CLOCK", x + 5, y + 15);
        g.drawString("RESET", x + 5, y + 35);
        g.drawString("MEMORY", x + 5, y + 65);
        g.drawString("ADDRESS", x + 5, y + 85);
        g.drawString("STDIN", x + 119, y + 15);
        g.drawString("STDOUT", x + 105, y + 35);
        painter.drawPorts();
    }

    @Override
    public void propagate(InstanceState state) {
        Value Clock = state.getPort(CLOCK);
        Value Reset = state.getPort(RESET);
        Value Memory = state.getPort(MEMORY);
        // Value Address = state.getPort(ADDRESS); - DEAD CODE
        // Value Stdin = state.getPort(STDIN);     - DEAD CODE
        // Value Stdout = state.getPort(STDOUT);   - DEAD CODE
        Value oldValue = lastClock;
        lastClock = Clock;
        if (Reset == Value.TRUE) {
            for (int i = 0; i < 16; i++) {
                registers[i].reset();
            }
            compareA = 0;
            compareB = 0;
            programCounter = 0;
        }
        if (oldValue == Value.FALSE && Clock == Value.TRUE) {
            if (taskQueue.isEmpty()) {
                if (programCounter > 65535) {
                    programCounter = 65535;
                }
                state.setPort(ADDRESS, Value.createKnown(BitWidth.create(17), 0x10000 | programCounter++), DELAY);
                state.setPort(MEMORY, Value.createUnknown(BitWidth.create(32)), DELAY);
                taskQueue.add(new Task(TaskType.FETCH, new Object[]{}));
            } else {
                Task task = taskQueue.poll();
                switch (task.getType()) {
                    case FETCH:
                        int instruction = Memory.toIntValue();
                        int opCode = instruction >>> 24;
                        int operand = instruction & 0x00FFFFFF;
                        boolean isHigh = (operand & 0x0000000F) == 0xF;
                        switch (opCode) {
                            case 0x01:
                                registers[(operand & 0x0000F000) >> 12].set(registers[(operand & 0x00F00000) >> 20].get() + registers[(operand & 0x000F0000) >> 16].get());
                                break;
                            case 0x02:
                                registers[(operand & 0x0000F000) >> 12].set(registers[(operand & 0x00F00000) >> 20].get() - registers[(operand & 0x000F0000) >> 16].get());
                                break;
                            case 0x03:
                                registers[(operand & 0x0000F000) >> 12].set(registers[(operand & 0x00F00000) >> 20].get() * registers[(operand & 0x000F0000) >> 16].get());
                                break;
                            case 0x04:
                                registers[(operand & 0x0000F000) >> 12].set(registers[(operand & 0x00F00000) >> 20].get() / registers[(operand & 0x000F0000) >> 16].get());
                                break;
                            case 0x05:
                                registers[(operand & 0x0000F000) >> 12].set(registers[(operand & 0x00F00000) >> 20].get() % registers[(operand & 0x000F0000) >> 16].get());
                                break;
                            case 0x06:
                                registers[(operand & 0x0000F000) >> 12].set(registers[(operand & 0x00F00000) >> 20].get() & registers[(operand & 0x000F0000) >> 16].get());
                                break;
                            case 0x07:
                                registers[(operand & 0x0000F000) >> 12].set(registers[(operand & 0x00F00000) >> 20].get() | registers[(operand & 0x000F0000) >> 16].get());
                                break;
                            case 0x08:
                                registers[(operand & 0x000F0000) >> 16].set(~registers[(operand & 0x00F00000) >> 20].get());
                                break;
                            case 0x09:
                                registers[(operand & 0x0000F000) >> 12].set(registers[(operand & 0x00F00000) >> 20].get() ^ registers[(operand & 0x000F0000) >> 16].get());
                                break;
                            case 0x0A:
                                registers[(operand & 0x0000F000) >> 12].set(registers[(operand & 0x00F00000) >> 20].get() << registers[(operand & 0x000F0000) >> 16].get());
                                break;
                            case 0x0B:
                                registers[(operand & 0x0000F000) >> 12].set(registers[(operand & 0x00F00000) >> 20].get() >>> registers[(operand & 0x000F0000) >> 16].get());
                                break;
                            case 0x0C:
                                programCounter = registers[(operand & 0x00F00000) >> 20].get();
                                break;
                            case 0x0D:
                                compareA = (long) registers[(operand & 0x00F00000) >> 20].get();
                                compareB = (long) registers[(operand & 0x000F0000) >> 16].get();
                                break;
                            case 0x0E:
                                if (compareA > compareB)
                                    programCounter = registers[(operand & 0x00F00000) >> 20].get();
                                break;
                            case 0x0F:
                                if (compareA >= compareB)
                                    programCounter = registers[(operand & 0x00F00000) >> 20].get();
                                break;
                            case 0x10:
                                if (compareA < compareB)
                                    programCounter = registers[(operand & 0x00F00000) >> 20].get();
                                break;
                            case 0x11:
                                if (compareA <= compareB)
                                    programCounter = registers[(operand & 0x00F00000) >> 20].get();
                                break;
                            case 0x12:
                                if (compareA == compareB)
                                    programCounter = registers[(operand & 0x00F00000) >> 20].get();
                                break;
                            case 0x13:
                                if (compareA != compareB)
                                    programCounter = registers[(operand & 0x00F00000) >> 20].get();
                                break;
                            case 0x14:
                                if (isHigh)
                                    registers[(operand & 0x00F00000) >> 20].set(registers[(operand & 0x00F00000) >> 20].get() | (operand & 0x000FFFF0) << 12);
                                else
                                    registers[(operand & 0x00F00000) >> 20].set(registers[(operand & 0x00F00000) >> 20].get() | (operand & 0x000FFFF0) >> 4);
                                break;
                            case 0x15:
                                registers[(operand & 0x000F0000) >> 16].set(registers[(operand & 0x00F00000) >> 20].get());
                                break;
                            case 0x16:
                                taskQueue.add(new Task(TaskType.SET_A, new Object[]{(operand & 0x000000F0) >> 4}));
                                state.setPort(ADDRESS, Value.createKnown(BitWidth.create(17), 0x10000 | (operand & 0x00FFFF00) >> 8), DELAY);
                                state.setPort(MEMORY, Value.createUnknown(BitWidth.create(32)), DELAY);
                                break;
                            case 0x17:
                                taskQueue.add(new Task(TaskType.WRITE, new Object[]{
                                        (operand & 0x000FFFF0) >> 4, registers[(operand & 0x00F00000) >> 20].get()
                                }));
                                break;
                            case 0x18:
                                taskQueue.add(new Task(TaskType.SET_A, new Object[]{(operand & 0x000F0000) >> 16}));
                                state.setPort(ADDRESS, Value.createKnown(BitWidth.create(17), 0x10000 | registers[(operand & 0x00F00000) >> 20].get()), DELAY);
                                state.setPort(MEMORY, Value.createUnknown(BitWidth.create(32)), DELAY);
                                break;
                            case 0x19:
                                taskQueue.add(new Task(TaskType.WRITE, new Object[]{
                                        registers[(operand & 0x000F0000) >> 16].get(), registers[(operand & 0x00F00000) >> 20].get()
                                }));
                                break;
                            case 0x1A:
                                registers[(operand & 0x00F00000) >> 20].set(state.getPort(STDIN).toIntValue());
                                break;
                            case 0x1B:
                                state.setPort(STDOUT, Value.createKnown(BitWidth.create(32), registers[(operand & 0x00F00000) >> 20].get()), DELAY);
                                break;
                        }
                        break;
                    case WRITE:
                        state.setPort(ADDRESS, Value.createKnown(BitWidth.create(17), (int) task.getData()[0]), DELAY);
                        state.setPort(MEMORY, Value.createKnown(BitWidth.create(32), (int) task.getData()[1]), DELAY);
                        break;
                    case SET_A:
                        registers[(int) task.getData()[0]].set(Memory.toIntValue());
                        break;
                }
            }
        }
    }
}