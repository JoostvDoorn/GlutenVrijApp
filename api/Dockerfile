FROM joostvdoorn/torchbaseimage
MAINTAINER Joost van Doorn <joost.van.doorn@gmail.com>

RUN luarocks install luasocket
RUN luarocks install https://raw.githubusercontent.com/msva/lua-htmlparser/master/htmlparser-scm-0.rockspec
RUN luarocks install https://raw.githubusercontent.com/benglard/htmlua/master/htmlua-scm-1.rockspec
RUN luarocks install https://raw.githubusercontent.com/benglard/waffle/master/waffle-scm-1.rockspec

RUN mkdir -p /etc/my_init.d
RUN mkdir -p /home/GlutenVrijApp/
ADD start.sh /etc/my_init.d/startServer.sh
RUN chmod 777 /etc/my_init.d/startServer.sh

WORKDIR /home/
RUN git clone https://github.com/JoostvDoorn/GlutenVrijApp
WORKDIR /home/GlutenVrijApp/api


# private expose
EXPOSE 80
EXPOSE 443
