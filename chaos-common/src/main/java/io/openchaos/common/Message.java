/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.openchaos.common;

public class Message {
    public String shardingKey;
    public byte[] payload;
    public long sendTimestamp;
    public long receiveTimestamp;
    public String extraInfo;

    public Message(byte[] payload) {
        this.payload = payload;
    }

    public Message(String shardingKey, byte[] payload) {
        this.shardingKey = shardingKey;
        this.payload = payload;
    }

    public Message(String shardingKey, byte[] payload, String extraInfo) {
        this.shardingKey = shardingKey;
        this.payload = payload;
        this.extraInfo = extraInfo;
    }

    public Message(String shardingKey, byte[] payload, long sendTimestamp, long receiveTimestamp,
        String extraInfo) {
        this.shardingKey = shardingKey;
        this.payload = payload;
        this.sendTimestamp = sendTimestamp;
        this.receiveTimestamp = receiveTimestamp;
        this.extraInfo = extraInfo;
    }
}
