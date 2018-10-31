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
    private Queue<Task> taskQueue = new LinkedList<>();
    private Register[] registers = new Register[16];
    private int programCounter = 0;

    private Value lastClock = Value.FALSE;

    private static final int CLOCK          = 0;
    private static final int RESET          = 1;
    private static final int MEMORY         = 2;
    private static final int ADDRESS        = 3;
    private static final int STDIN          = 4;
    private static final int STDOUT         = 5;

    private static final int DELAY = 50;

    Javanilla() {
        super("Ja-Vanilla");

        setAttributes(new Attribute[]{

        }, new Object[]{

        });

        setOffsetBounds(Bounds.create(0, 0, 160, 90));

        setPorts(new Port[]{
                new Port(0, 10, Port.INPUT, 1),    // CLOCK
                new Port(0, 30, Port.INPUT, 1),    // RESET
                new Port(0, 60, Port.INOUT, 32),   // MEMORY
                new Port(0, 80, Port.OUTPUT, 17),  // ADDRESS
                new Port(160, 10, Port.INPUT, 32), // STDIN
                new Port(160, 30, Port.OUTPUT, 32) // STDOUT
        });
    }

    @Override
    public void paintInstance(InstancePainter painter) {
        Graphics g = painter.getGraphics();
        Bounds b = painter.getBounds();

        int x = painter.getBounds().getX();
        int y = painter.getBounds().getY();

        AttributedString projectName = new AttributedString("Ja-Vanilla");
        AttributedString authorName = new AttributedString("by Yottabyte090");
        projectName.addAttribute(TextAttribute.FOREGROUND, Color.gray);
        authorName.addAttribute(TextAttribute.FOREGROUND, Color.gray);

        painter.drawRectangle(painter.getBounds(), "");

        //g.drawString("이 프로젝트는 Yottabyte090이 제작한 Ja-Vanilla를 사용합니다.", 10, 20);
        g.drawString(projectName.getIterator(), x + 101, y + 72);
        g.drawString(authorName.getIterator(), x + 70, y + 85);
        g.drawString("CLOCK", x + 5, y + 15);
        g.drawString("RESET", x + 5 , y + 35);
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
        Value Address = state.getPort(ADDRESS);
        Value Stdin = state.getPort(STDIN);
        Value Stdout = state.getPort(STDOUT);

        Value oldValue = lastClock;
        lastClock = Clock;

        if(Reset == Value.TRUE){
            for(Register register : registers){
                register.reset();
            }

            programCounter = 0;
        }

        if(oldValue == Value.FALSE && Clock == Value.TRUE){
            if(taskQueue.isEmpty()){
                if(programCounter > 65535){
                    programCounter = 65535;
                }

                state.setPort(ADDRESS, Value.createKnown(BitWidth.create(17), 0x10000 | programCounter ++), DELAY);
                state.setPort(MEMORY, Value.createUnknown(BitWidth.create(32)), DELAY);

                taskQueue.add(new Task(TaskType.FETCH, new Object[]{ }));
            }else{
                Task task = taskQueue.poll();

                switch(task.getType()){
                    case FETCH:
                        int instruction = Memory.toIntValue();
                        decode(state, instruction);

                        break;
                    case WRITE:
                        int address = (int) task.getData()[0];
                        int value = (int) task.getData()[1];

                        state.setPort(ADDRESS, Value.createKnown(BitWidth.create(17), address), DELAY);
                        state.setPort(MEMORY, Value.createKnown(BitWidth.create(32), value), DELAY);
                        break;
                }
            }
        }
    }

    private void decode(InstanceState state, int instruction){
        int opCode = instruction >>> 24;
        int operand = (opCode << 24) ^ instruction;

        taskQueue.add(new Task(TaskType.WRITE, new Object[]{ programCounter + 1, operand }));
    }
}