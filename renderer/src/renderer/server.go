package main

import (
	"log"
	"github.com/fogleman/pt/pt"
	"golang.org/x/net/context"
	"google.golang.org/grpc"
	"image"
	"bytes"
	"image/jpeg"
	pb "renderer/proto"
	"net"
	"math/rand"
)
type Renderer struct {}

const (
	port = ":50052"
)

func (s *Renderer) Render(ctx context.Context,req *pb.RenderRequest) (*pb.RenderResponse, error) {
	scene := pt.Scene{}
	wall := pt.SpecularMaterial(pt.HexColor(0x2FC92C),8)
	//texture, err := pt.LoadTexture("valentyn.jpg")
	img, _, err := image.Decode(bytes.NewReader(req.Texture))
	texture := pt.NewTexture(img)
	if err != nil {
		return nil,err
	}
	wall.Texture = texture
	scene.Add(pt.NewSphere(pt.Vector{1.5, 1, 0}, 1, pt.SpecularMaterial(pt.HexColor(0x334D5C), 8)))
	scene.Add(pt.NewSphere(pt.Vector{-1, 1, 2}, 1, wall))
	scene.Add(pt.NewSphere(pt.Vector{-3, 1, 0.5}, 1, wall))
	scene.Add(pt.NewSphere(pt.Vector{-0.5, 0.5, -1.4}, 0.7, wall))
	scene.Add(pt.NewCube(pt.Vector{-100, -1, -100}, pt.Vector{100, 0, 100}, pt.DiffuseMaterial(pt.Color{1, 1, 1})))
	scene.Add(pt.NewSphere(pt.Vector{-1, 4, -1}, 0.5, pt.LightMaterial(pt.Color{1, 1, 1}, 3, pt.LinearAttenuation(1))))
	camera := pt.LookAt(pt.Vector{-1.5+rand.Float64()*2, 2+rand.Float64()*2, -5}, pt.Vector{0, 0, 3}, pt.Vector{0, 1, 0}, 45)
	im := pt.Render(&scene, &camera, 320, 240, 4, 16, 18)
	buf := new(bytes.Buffer)
	err = jpeg.Encode(buf, im, nil)
	if err != nil {
		return nil,err
	}
	return &pb.RenderResponse{Image:buf.Bytes()},nil
}

func main() {
	lis, err := net.Listen("tcp", port)
	if err != nil {
		log.Fatalf("failed to listen: %v", err)
	}
	s := grpc.NewServer()
	pb.RegisterRendererServer(s, &Renderer{})
	log.Printf("Starting rendering server on port %v\n",port)
	s.Serve(lis)
}
