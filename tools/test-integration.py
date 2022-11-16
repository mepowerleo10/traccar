#!/usr/bin/python

import base64
import sys
import os
import xml.etree.ElementTree
import urllib.request, urllib.parse, urllib.error
import urllib.request, urllib.error, urllib.parse
import json
import socket
import time
import threading

messages = {
    'gps103' : 'imei:123456789012345,help me,1201011201,,F,120100.000,A,6000.0000,N,13000.0000,E,0.00,;',
    'tk103' : '(123456789012BP05123456789012345120101A6000.0000N13000.0000E000.0120200000.0000000000L000946BB)',
    'gl100' : '+RESP:GTSOS,123456789012345,0,0,0,1,0.0,0,0.0,1,130.000000,60.000000,20120101120300,0460,0000,18d8,6141,00,11F0,0102120204\0',
    'gl200' : '+RESP:GTFRI,020102,123456789012345,,0,0,1,1,0.0,0,0.0,130.000000,60.000000,20120101120400,0460,0000,18d8,6141,00,,20120101120400,11F0$',
    't55' : '$PGID,123456789012345*0F\r\n$GPRMC,120500.000,A,6000.0000,N,13000.0000,E,0.00,0.00,010112,,*33\r\n',
    'xexun' : '111111120009,+436763737552,GPRMC,120600.000,A,6000.0000,N,13000.0000,E,0.00,0.00,010112,,,A*68,F,, imei:123456789012345,04,481.2,F:4.15V,0,139,2689,232,03,2725,0576\n',
    'totem' : '$$B3123456789012345|AA$GPRMC,120700.000,A,6000.0000,N,13000.0000,E,0.00,,010112,,,A*74|01.8|01.0|01.5|000000000000|20120403234603|14251914|00000000|0012D888|0000|0.0000|3674|940B\r\n',
    'meiligao' : '$$\x00\x60\x12\x34\x56\xFF\xFF\xFF\xFF\x99\x55120900.000,A,6000.0000,N,13000.0000,E,0.00,,010112,,*1C|11.5|194|0000|0000,0000\x69\x62\x0D\x0A',
    'suntech' : 'SA200STT;123456;042;20120101;12:11:00;16d41;-15.618767;-056.083214;000.011;000.00;11;1;41557;12.21;000000;1;3205\r',
    'h02' : '*HQ,123456789012345,V1,121300,A,6000.0000,N,13000.0000,E,0.00,0.00,010112,ffffffff,000000,000000,000000,000000#',
    'jt600' : '$\x00\x00\x12\x34\x56\x11\x00\x1B\x01\x01\x12\x12\x14\x00\x60\x00\x00\x00\x13\x00\x00\x00\x0F\x00\x00\x07\x50\x00\x00\x00\x2B\x91\x04\x4D\x1F\xA1',
    'v680' : '#123456789012345#1000#0#1000#AUT#1#66830FFB#13000.0000,E,6000.0000,N,001.41,259#010112#121600##',
    'pt502' : '$POS,123456,121700.000,A,6000.0000,N,13000.0000,E,0.0,0.0,010112,,,A/00000,00000/0/23895000//\r\n',
    'tr20' : '%%123456789012345,A,120101121800,N6000.0000E13000.0000,0,000,0,01034802,150,[Message]\r\n',
    'meitrack' : '$$d138,123456789012345,AAA,35,60.000000,130.000000,120101122000,A,7,18,0,0,0,49,3800,24965,510|10|0081|4F4F,0000,000D|0010|0012|0963|0000,,*BF\r\n',
    'megastek' : 'STX,102110830074542,$GPRMC,122400.000,A,6000.0000,N,13000.0000,E,0.00,0.00,010112,,,A*64,F,LowBattery,imei:123456789012345,03,113.1,Battery=24%,,1,460,01,2531,647E;57\r\n',
    'gpsgate' : '$FRLIN,IMEI,123456789012345,*7B\r\n$GPRMC,122600.000,A,6000.00000,N,13000.00000,E,0.000,0.0,010112,,*0A\r\n',
    'tlt2h' : '#123456789012345#V500#0000#AUTO#1\r\n#$GPRMC,123000.000,A,6000.0000,N,13000.0000,E,0.00,0.00,010112,,,D*70\r\n##\r\n',
    'taip' : '>REV481669045060+6000000-1300000000000012;ID=123456789012345<',
    'wondex' : '123456789012345,20120101123200,130.000000,60.000000,0,000,0,0,2\r\n',
    'ywt' : '%RP,123456789012345:0,120101123500,E130.000000,N60.000000,,0,0,4,0,00\r\n',
    'tk102' : '[!0000000081r(123456789012345,TK102-W998_01_V1.1.001_130219,255,001,255,001,0,100,100,0,internet,0000,0000,0,0,255,0,4,1,11,00)][=00000000836(ITV123600A6000.0000N13000.0000E000.00001011210010000)]',
    'wialon' : '#L#123456789012345;test\r\n#SD#010112;123900;6000.0000;N;13000.0000;E;0;0;0;4\r\n',
    'carscop' : '*040331141830UB05123456789012345010112A6000.0000N13000.0000E000.0124000000.0000000000L000000^',
    'manpower' : 'simei:123456789012345,,,tracker,51,24,1.73,120101124200,A,6000.0000,N,13000.0000,E,0.00,28B9,1DED,425,01,1x0x0*0x1*60x+2,en-us,;',
    'globalsat' : '$123456789012345,1,1,010112,124300,E13000.0000,N6000.0000,00000,0.0100,147,07,2.4!',
    'pt3000' : '%123456789012345,$GPRMC,124500.000,A,6000.0000,N,13000.0000,E,0.00,,010112,,,A,+100000000000,N098d',
    'topflytech' : '(123456789012345BP00XG00b600000000L00074b54S00000000R0C0F0014000100f0120101124700A6000.0000N13000.0000E000.0000.00)',
    'laipac' : '$AVRMC,123456789012345,124800,a,6000.0000,N,13000.0000,E,0.00,0.00,010112,0,3.727,17,1,0,0*17\r\n',
    'gotop' : '#123456789012345,CMD-T,A,DATE:120101,TIME:125000,LAT:60.0000000N,LOT:130.0000000E,Speed:000.0,84-20,000#',
    'sanav' : 'imei:123456789012345rmc:$GPRMC,093604.354,A,4735.0862,N,01905.2146,E,0.00,0.00,171013,,*09,AUTO-4103mv',
    'easytrack' : '*ET,123456789012345,DW,A,0A090D,101C0D,00CF27C6,0413FA4E,0000,0000,00000000,20,4,0000,00F123#',
    'gpsmarker' : '$GM200123456789012345T100511123300N55516789E03756123400000035230298#\r',
    'stl060' : '$1,123456789012345,D001,AP29AW0963,23/02/14,14:06:54,17248488N,078342226E,0.08,193.12,1,1,1,1,1,A#',
    'cartrack' : '$$123456????????&A9955&B102904.000,A,2233.0655,N,11404.9440,E,0.00,,030109,,*17|6.3|&C0100000100&D000024?>&E10000000##',
    'minifinder' : '!1,123456789012345;!A,01/01/12,12:15:00,60.000000,130.000000,0.0,25101,0;',
    'haicom' : '$GPRS123456789012345,T100001,150618,230031,5402267400332464,0004,2014,000001,,,1,00#V040*',
    'box' : 'H,BT,123456789012345,081028142432,F5813D19,6D6E6DC2\rL,081028142429,G,52.51084,-1.70849,0,170,0,1,0\r',
    'freedom' : 'IMEI,123456789012345,2014/05/22, 20:49:32, N, Lat:4725.9624, E, Lon:01912.5483, Spd:5.05\r\n',
    'telic' : '182012345699,010100001301,0,270613041652,166653,475341,3,0,355,6,2,1,231,8112432,23201,01,00,217,0,0,0,0,7\0',
    'trackbox' : 'a=connect&v=11&i=123456789012345\r\n183457.999,5126.0247N,00002.8686E,5.2,70.4,3,57.63,32.11,17.32,150507,05\r\n',
    'visiontek' : '$1,AP09BU9397,123456789012345,20,06,14,15,03,28,17267339N,078279407E,060.0,073,0550,11,0,1,0,0,1,1,26,A,0000000000#',
    'tr900' : '>123456,4,1,150626,131252,W05830.2978,S3137.2783,,00,348,18,00,003-000,0,3,11111011*3b!\r\n',
    'ardi01' : '123456789012345,20141010052719,24.4736042,56.8445807,110,289,40,7,5,78,-1\r\n',
    'xt013' : 'TK,123456789012345,150131090859,+53.267863,+5.767363,0,38,12,0,F,204,08,C94,336C,24,,4.09,1,,,,,,,,\r\n',
    'gosafe' : '*GS16,123456789012345,100356130215,,SYS:G79W;V1.06;V1.0.2,GPS:A;6;N24.802700;E46.616828;0;0;684;1.35,COT:60,ADC:4.31;0.10,DTT:20000;;0;0;0;1#',
    'xirgo' : '$$123456789012345,6001,2013/01/22,15:36:18,25.80907,-80.32531,7.1,19,165.2,11,0.8,11.1,17,1,1,3.9,2##',
    'mtx' : '#MTX,123456789012345,20101226,195550,41.6296399,002.3611174,000,035,000000.00,X,X,1111,000,0,0\r\n',
    'aquila' : '$$SRINI_1MS,123456,1,12.963515,77.533844,150925161628,A,27,0,8,0,68,0,0,0,0,0,0,0,0,1,0,1,1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1,*43\r\n',
    'flextrack' : '-1,LOGON,123456,8945000000\r-2,UNITSTAT,20060101,123442,1080424008,N0.00.0000,E0.00.0000,0,0,0,4129,-61,2,23866,0,999,A214,63,2EE2,3471676\r',
    'watch' : '[3G*1234567890*00FD*UD,160617,120759,V,18.917990,S,47.5450083,E,0.00,0.0,0.0,0,87,1,0,0,00000011,7,255,646,2,81,11552,140,81,10281,127,81,10602,127,81,11553,126,81,10284,121,81,11122,119,81,10662,119,2,NETGEAR20,44:94:fc:43:5b:78,-47,TP-LINK_7650,98:de:d0:46:76:50,-88,46.5]',
    'upro' : '*AI2001234567890,BA&A2003064913201201845107561627121016&B0100000000&C05>8=961&F0333&K023101002154A7#',
    'auro' : 'M0030T0000816398975I123456789012345E00001W*****110620150441000068DA#RD01DA240000000000+100408391+013756125100620152140102362238320034400\r\n',
    'disha' : '$A#A#123456789012345#A#053523#010216#2232.7733#N#08821.1940#E#002.75#038.1#09#00.8#1800#0#000#0000#9999#11.7#285.7#0001*\r\n',
    'arnavi' : '$AV,V3,123456,12487,2277,203,65534,0,0,193,65535,65535,65535,65535,1,13,200741,5950.6773N,03029.1043E,300.0,360.0,121012,65535,65535,65535,SF*6E\r\n',
    'kenji' : '>C123456,M005004,O0000,I0002,D124057,A,S3137.2783,W05830.2978,T000.0,H254.3,Y240116,G06*17\r\n',
    'fox' : '<fox><gps id="123456" data="0,A,110316,131834,4448.8355,N,02028.2021,E,0,217,,1111111111111111 123 0 0 0 0 0 00000000 50020,50020 0"/></fox>',
    'gnx' : '$GNX_DIO,123456789012345,110,1,155627,121214,151244,121214,1,08.878321,N,076.643154,E,0,0,0,0,0,0,GNX01001,B1*\n\r',
    'arknav' : '123456789012345,05*850,000,L001,A,2459.3640,N,12125.2958,E,000.0,224.8,00.8,07:47:26 09-09-05,9.00,D3,0,C4,1,,,,\r',
    'supermate' : '2:123456789012345:1:*,00000000,XT,A,10031b,140b28,80ad4c72,81ba2d2c,06ab,238c,020204010000,12,0,0000,0003e6#',
    'appello' : 'FOLLOWIT,123456789012345,160211221959,-12.112660,-77.045258,1,0,6,116,F,716,17,4E85,050C,29,,4.22,,39,999/00/00,,,,,,46206,\r\n',
    'idpl' : '*ID1,123456789012345,210314,162752,A,1831.4412,N,07351.0983,E,0.04,213.84,9,25,A,1,4.20,0,1,01,1,0,0,A01,L,EA01#\r\n',
    'hunterpro' : '>1234<$GPRMC,170559.000,A,0328.3045,N,07630.0735,W,0.73,266.16,200816,,,A77, s000078015180",0MD\r',
    'raveon' : '$PRAVE,1234,0001,3308.9051,-11713.1164,000000,1,10,168,31,13.3,3,-83,0,0,,1003.4*66\r\n',
    'cradlepoint' : '123456789012,000000,4337.174385,N,11612.338373,W,0.0,,Verizon,,-71,-44,-11,,\r\n',
    'arknavx8' : '123456789012345,241111;1R,110509053244,A,2457.9141N,12126.3321E,220.0,315,10.0,00000000;',
    'autograde' : '(000000007322123456789012345170415A1001.1971N07618.1375E0.000145312128.59?A0024B0024C0000D0000E0000K0000L0000M0000N0000O0000)',
    'cguard' : 'IDRO:123456789012345\r\nNV:170409 031456:56.808553:60.595476:0:NAN:0\r\n',
    'fifotrack' : '$$105,123456789012345,AB,A00,,161007085534,A,54.738791,25.271918,0,350,151,0,17929,0000,0,,246|1|65|96DB,936|0*0B\r\n',
    'extremtrac' : '$GPRMC,123456789012345,050859.000,A,1404.8573,N,08710.9967,W,0.00,0,080117,0,,00C8,00218,99,,,,,,0.00\r\n',
    'trakmate' : '^TMSRT|123456789012345|12.59675|77.56789|123456|030414|1.03|1.01|#',
    'maestro' : '@123456789012345,601,UPV-02,0,13.4,10,0,0,16/11/04,17:21:14,0.352793,32.647927,0,0,0,0,99,0.000,0!\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0',
    'gt30' : '$$005D1234567890    9955102834.000,A,3802.8629,N,02349.7163,E,0.00,,060117,,*13|1.3|26225BD\r\n',
    'tmg' : '$nor,L,123456789012345,24062015,094459,4,2826.1956,N,07659.7690,E,67.5,2.5,167,0.82,15,22,airtel,31,4441,1,4.1,12.7,00000011,00000011,1111,0.0,0.0,21.3,SW00.01,#\r\n',
    'pretrace' : '(123456789012345U1110A1701201500102238.1700N11401.9324E000264000000000009001790000000,&P11A4,F1050^47)',
    'siwi' : '$SIWI,1234,1320,A,0,,1,1,0,0,876578,43,10,A,19.0123456,72.65347,45,0,055929,071107,22,5,1,0,3700,1210,0,2500,1230,321,0,1.1,4.0,1!\r\n',
    'starlink' : '$SLU123456,06,622,170329035057,01,170329035057,+3158.0018,+03446.6968,004.9,007,000099,1,1,0,0,0,0,0,0,,,14.176,03.826,,1,1,1,4*B0\r\n',
    'alematics' : '$T,50,592,123456789012345,20170515062915,20170515062915,25.035005,121.561555,0,31,89,3.7,5,1,0,0.000,12.752,1629,38,12752,4203,6\r\n',
    'vtfms' : '(123456789012345,00I76,00,000,,,,,A,133755,210617,10.57354,077.24912,SW,000,00598,00000,K,0017368,1,12.7,,,0.000,,,0,0,0,0,1,1,0,,)074'
}

baseUrl = 'http://localhost:8080'
user = { 'email' : 'admin', 'password' : 'admin' }

debug = '-v' in sys.argv

def load_ports():
    ports = {}
    dir = os.path.dirname(os.path.abspath(__file__))
    root = xml.etree.ElementTree.parse(dir + '/../setup/default.xml').getroot()
    for entry in root.findall('entry'):
        key = entry.attrib['key']
        if key.endswith('.port'):
            ports[key[:-5]] = int(entry.text)
    if debug:
        print('\nports: %s\n' % repr(ports))
    return ports

def login():
    request = urllib.request.Request(baseUrl + '/api/session')
    response = urllib.request.urlopen(request, urllib.parse.urlencode(user).encode("utf-8"))
    if debug:
        print('\nlogin: %s\n' % repr(json.load(response)))
    return response.headers.get('Set-Cookie')

def remove_devices(cookie):
    request = urllib.request.Request(baseUrl + '/api/devices')
    request.add_header('Cookie', cookie)
    response = urllib.request.urlopen(request)
    data = json.load(response)
    if debug:
        print('\ndevices: %s\n' % repr(data))
    for device in data:
        request = urllib.request.Request(baseUrl + '/api/devices/' + str(device['id']))
        request.add_header('Cookie', cookie)
        request.get_method = lambda: 'DELETE'
        response = urllib.request.urlopen(request)

def add_device(cookie, unique_id):
    request = urllib.request.Request(baseUrl + '/api/devices')
    request.add_header('Cookie', cookie)
    request.add_header('Content-Type', 'application/json')
    device = { 'name' : unique_id, 'uniqueId' : unique_id }
    response = urllib.request.urlopen(request, json.dumps(device).encode("utf-8"))
    data = json.load(response)
    return data['id']

def send_message(port, message):
    s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    s.connect(('127.0.0.1', port))
    s.send(message)
    time.sleep(5)
    s.close()

def get_protocols(cookie, device_id):
    params = { 'deviceId' : device_id, 'from' : '2000-01-01T00:00:00.000Z', 'to' : '2050-01-01T00:00:00.000Z' }
    request = urllib.request.Request(baseUrl + '/api/positions?' + urllib.parse.urlencode(params))
    request.add_header('Cookie', cookie)
    request.add_header('Content-Type', 'application/json')
    request.add_header('Accept', 'application/json')
    response = urllib.request.urlopen(request)
    protocols = []
    for position in json.load(response):
        protocols.append(position['protocol'])
    return protocols

if __name__ == "__main__":
    ports = load_ports()

    cookie = login()
    remove_devices(cookie)

    devices = {
        '123456789012345' : add_device(cookie, '123456789012345'),
        '123456789012' : add_device(cookie, '123456789012'),
        '1234567890' : add_device(cookie, '1234567890'),
        '123456' : add_device(cookie, '123456'),
        '1234' : add_device(cookie, '1234')
    }

    all = set(ports.keys())
    protocols = set(messages.keys())

    print('Total: %d' % len(all))
    print('Missing: %d' % len(all - protocols))
    print('Covered: %d' % len(protocols))

    #if all - protocols:
    #    print '\nMissing: %s\n' % repr(list((all - protocols)))

    for protocol in messages:
        thread = threading.Thread(target = send_message, args = (ports[protocol], messages[protocol].encode("utf-8")))
        thread.start()

    time.sleep(10)

    for device in devices:
        protocols -= set(get_protocols(cookie, devices[device]))

    print('Success: %d' % (len(messages) - len(protocols)))
    print('Failed: %d' % len(protocols))

    if protocols:
        print('\nFailed: %s' % repr(list(protocols)))
